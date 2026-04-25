#!/usr/bin/env python3
"""Extract a pyVmomi managed-object schema snapshot as JSON.

Usage:
    pip install --upgrade pyvmomi
    python3 tools/extract_pyvmomi_schema.py > esx/pyvmomi-schema-<version>.json
"""
import json
import sys
from datetime import datetime, timezone

import pyVmomi
from pyVmomi import vim, VmomiSupport


SCHEMA_VERSION = 1
NAMESPACE = "vim"

# The four MOs yavijava treats as hand-written infrastructure; we still emit
# them so the JSON is complete, but the generator's MIGRATION_EXCLUDE skips them.
INFRASTRUCTURE = {"ManagedObject", "ManagedEntity", "ExtensibleManagedObject", "View"}


def main() -> int:
    output = {
        "schemaVersion": SCHEMA_VERSION,
        "extractedAt": datetime.now(timezone.utc).isoformat(),
        "pyvmomiVersion": pyvmomi_version_string(),
        "vimNamespace": NAMESPACE,
        "managedObjects": sorted(
            (extract_mo(qname) for qname in iter_managed_qnames()),
            key=lambda m: m["name"],
        ),
    }
    json.dump(output, sys.stdout, indent=2, sort_keys=True)
    sys.stdout.write("\n")
    return 0


def pyvmomi_version_string() -> str:
    """Best-effort version string. pyVmomi exposes __version__ on recent releases;
    older releases require fallback."""
    return getattr(pyVmomi, "__version__", "unknown")


def iter_managed_qnames():
    """Yield qualified names like 'vim.VirtualMachine' that live in our target namespace."""
    for qname in sorted(VmomiSupport._managedDefMap):
        if qname == "ManagedObject":
            continue  # synthesized root, no real schema
        if qname.startswith(f"{NAMESPACE}."):
            yield qname


def extract_mo(qname: str) -> dict:
    cls = resolve_type(qname)
    return {
        "name": cls._wsdlName,
        "wsdlName": cls._wsdlName,
        "qualifiedName": qname,
        "parent": parent_wsdl_name(cls),
        "version": getattr(cls, "_version", None),
        "introducedIn": getattr(cls, "_version", None),
        "properties": sorted(
            (extract_property(name, info) for name, info in cls._propInfo.items()),
            key=lambda p: p["name"],
        ),
        "methods": sorted(
            (extract_method(name, info) for name, info in cls._methodInfo.items()),
            key=lambda m: m["name"],
        ),
    }


def resolve_type(qname: str):
    parts = qname.split(".")
    obj = pyVmomi
    for p in parts:
        obj = getattr(obj, p)
    return obj


def parent_wsdl_name(cls) -> str:
    if not cls.__bases__:
        return ""
    base = cls.__bases__[0]
    return getattr(base, "_wsdlName", base.__name__)


def extract_property(name, info) -> dict:
    # info.type is a type class directly (possibly an array class like Foo[])
    py_type = info.type
    return {
        "name": name,
        "type": resolve_wsdl_type(py_type),
        "isArray": is_array_type(py_type),
        "isOptional": bool(info.flags & 4),  # F_OPTIONAL = 4
        "isManagedObjectReference": is_mor_type(py_type),
        "version": info.version,
        "privilegeId": info.privId,
    }


def extract_method(name, info) -> dict:
    # info.result is the return type class directly (not a param object)
    # info.resultFlags & 4 indicates the result is an array
    result_type = info.result  # type class or NoneType
    return {
        "name": name,
        "wsdlName": info.wsdlName,
        "version": info.version,
        "params": [extract_param(p) for p in info.params],
        "returnType": method_return_type(result_type),
        "returnIsArray": method_return_is_array(result_type, info.resultFlags),
        "returnIsManagedObjectReference": method_return_is_mor(result_type),
        "faults": sorted(resolve_fault_type(f) for f in (info.faults or [])),
        "privilegeId": info.privId,
    }


def extract_param(p) -> dict:
    # p.type is a type class directly (possibly array class); p.flags & 4 = optional
    py_type = p.type
    return {
        "name": p.name,
        "type": resolve_wsdl_type(py_type),
        "isArray": is_array_type(py_type),
        "isOptional": bool(p.flags & 4),
        "isManagedObjectReference": is_mor_type(py_type),
    }


def method_return_type(result_type) -> str:
    if result_type is None or result_type is type(None):
        return "void"
    return resolve_wsdl_type(result_type)


def method_return_is_array(result_type, result_flags: int) -> bool:
    if result_type is None or result_type is type(None):
        return False
    # pyVmomi signals array return via resultFlags & 4, and the class name ends with []
    return bool(result_flags & 4) or is_array_type(result_type)


def method_return_is_mor(result_type) -> bool:
    if result_type is None or result_type is type(None):
        return False
    return is_mor_type(result_type)


# pyVmomi exposes primitive types using their Python names. Map them to the
# Java types yavijava uses on the wire side.
PRIMITIVE_PYTHON_TO_JAVA = {
    "str": "String",
    "bool": "boolean",
    "int": "int",
    "long": "long",
    "short": "short",
    "byte": "byte",
    "float": "float",
    "double": "double",
    "datetime": "Calendar",
    "object": "Object",
    "binary": "byte[]",
    "type": "String",  # vmodl type name; rendered as a String in yavijava
    "PropertyPath": "String",
    "TypeName": "String",
    "MethodName": "String",
    "URI": "String",
    "PEM": "String",
    "X509CertChain": "String",
}


def resolve_wsdl_type(py_type) -> str:
    """Translate a pyVmomi type class to its WSDL name.

    For array types (e.g. vim.VirtualMachine.Connection[]), unwrap to the
    base item type and return its wsdlName. Python primitive types
    (str, bool, datetime, etc.) are mapped to their Java equivalents.
    """
    if isinstance(py_type, str):
        # Shouldn't happen for property/param types, but handle defensively
        if "." in py_type:
            return py_type.rsplit(".", 1)[-1]
        return PRIMITIVE_PYTHON_TO_JAVA.get(py_type, py_type)

    # Array types have a .Item attribute pointing to the element type
    base = py_type
    if hasattr(base, "Item"):
        base = base.Item

    wsdl = getattr(base, "_wsdlName", None)
    if wsdl:
        return PRIMITIVE_PYTHON_TO_JAVA.get(wsdl, wsdl)

    # Fallback: use the unqualified class name, stripping [] suffix
    name = base.__name__
    if name.endswith("[]"):
        name = name[:-2]
    # Strip qualified prefix (e.g. 'vim.VirtualMachine.Connection' -> 'Connection')
    if "." in name:
        name = name.rsplit(".", 1)[-1]
    return PRIMITIVE_PYTHON_TO_JAVA.get(name, name)


def resolve_fault_type(fault_ref) -> str:
    """Fault entries in info.faults are strings like 'vim.fault.TaskInProgress'."""
    if isinstance(fault_ref, str):
        if "." in fault_ref:
            return fault_ref.rsplit(".", 1)[-1]
        return fault_ref
    # Might also be a type class
    return resolve_wsdl_type(fault_ref)


def is_array_type(py_type) -> bool:
    if isinstance(py_type, str):
        return False
    # Array wrapper classes have a .Item attribute
    return hasattr(py_type, "Item")


def is_mor_type(py_type) -> bool:
    if isinstance(py_type, str):
        return False
    # Unwrap array if needed
    base = py_type
    if hasattr(base, "Item"):
        base = base.Item
    # Walk MRO; a managed object inherits from vim.ManagedObject
    for c in getattr(base, "__mro__", ()):
        if getattr(c, "_wsdlName", None) == "ManagedObject":
            return True
    return False


if __name__ == "__main__":
    sys.exit(main())
