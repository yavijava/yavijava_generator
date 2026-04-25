package com.toastcoders.vmware.yavijava.data

import com.toastcoders.vmware.yavijava.pyvmomi.PyvmomiManagedObject
import com.toastcoders.vmware.yavijava.pyvmomi.PyvmomiSchema
import org.junit.Test

class ManagedObjectSingleClassTemplateTest {

    private PyvmomiManagedObject vm() {
        return new PyvmomiSchema().read(new File("src/test/resources/pyvmomi/sample-schema.json"))["VirtualMachine"]
    }

    @Test
    void testEmitsMarker() {
        String out = ManagedObjectSingleClassTemplate.render(vm())
        assert out.contains("auto generated using yavijava_generator")
    }

    @Test
    void testEmitsPackageAndImports() {
        String out = ManagedObjectSingleClassTemplate.render(vm())
        assert out.contains("package com.vmware.vim25.mo;")
        assert out.contains("import com.vmware.vim25.*;")
        assert out.contains("import java.rmi.RemoteException;")
    }

    @Test
    void testEmitsBothFences() {
        String out = ManagedObjectSingleClassTemplate.render(vm())
        assert out.contains("BEGIN custom imports (preserved by regenerator)")
        assert out.contains("END custom imports")
        assert out.contains("BEGIN custom (preserved by regenerator)")
        assert out.contains("END custom")
    }

    @Test
    void testEmitsClassHeaderWithExtends() {
        String out = ManagedObjectSingleClassTemplate.render(vm())
        assert out.contains("public class VirtualMachine extends ManagedEntity {")
    }

    @Test
    void testEmitsConstructor() {
        String out = ManagedObjectSingleClassTemplate.render(vm())
        assert out.contains("public VirtualMachine(ServerConnection serverConnection, ManagedObjectReference mor) {")
        assert out.contains("super(serverConnection, mor);")
    }

    @Test
    void testPropertyAccessorsAreAlphabetical() {
        String out = ManagedObjectSingleClassTemplate.render(vm())
        int posName    = out.indexOf("public String getName()")
        int posParent  = out.indexOf("public ManagedEntity getParent()")
        int posRecent  = out.indexOf("public Task[] getRecentTask()")
        int posRuntime = out.indexOf("public VirtualMachineRuntimeInfo getRuntime()")
        assert posName > 0 && posParent > 0 && posRecent > 0 && posRuntime > 0
        assert posName < posParent
        assert posParent < posRecent
        assert posRecent < posRuntime
    }

    @Test
    void testPrimitivePropertyShape() {
        String out = ManagedObjectSingleClassTemplate.render(vm())
        assert out.contains("public String getName() {")
        assert out.contains("return (String) getCurrentProperty(\"name\");")
    }

    @Test
    void testSingleMorPropertyShape() {
        String out = ManagedObjectSingleClassTemplate.render(vm())
        assert out.contains("public ManagedEntity getParent() {")
        assert out.contains("return (ManagedEntity) this.getManagedObject(\"parent\");")
    }

    @Test
    void testArrayMorPropertyShape() {
        // Task uses typed-helper getTasks
        String out = ManagedObjectSingleClassTemplate.render(vm())
        assert out.contains("public Task[] getRecentTask() {")
        assert out.contains("return getTasks(\"recentTask\");")
    }

    @Test
    void testVoidMethodShape() {
        String out = ManagedObjectSingleClassTemplate.render(vm())
        assert out.contains("public void reload() throws RuntimeFault, RemoteException {")
        assert out.contains("getVimService().reload(getMOR());")
    }

    @Test
    void testMorReturningMethodShape() {
        String out = ManagedObjectSingleClassTemplate.render(vm())
        assert out.contains(
            "public Task powerOnVM_Task(HostSystem host) throws FileFault, InvalidState, TaskInProgress, RuntimeFault, RemoteException {"
        )
        assert out.contains("ManagedObjectReference resultMor = getVimService().powerOnVM_Task(getMOR(), host == null ? null : host.getMOR());")
        assert out.contains("return new Task(getServerConnection(), resultMor);")
    }

    @Test
    void testMethodsAreAlphabetical() {
        String out = ManagedObjectSingleClassTemplate.render(vm())
        int posPower = out.indexOf("public Task powerOnVM_Task")
        int posReload = out.indexOf("public void reload")
        assert posPower > 0 && posReload > 0
        assert posPower < posReload
    }
}
