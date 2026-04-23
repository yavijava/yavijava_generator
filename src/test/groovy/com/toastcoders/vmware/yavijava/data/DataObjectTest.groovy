package com.toastcoders.vmware.yavijava.data

import org.junit.Test

public class DataObjectTest {

    @Test
    void testExtendsBaseDefaultIsEmpty() {
        DataObject obj = new DataObject()
        assert obj.extendsBase == ""
    }

    @Test
    void testNameDefaultIsNull() {
        DataObject obj = new DataObject()
        assert obj.name == null
    }

    @Test
    void testObjPropertiesDefaultIsEmptyList() {
        DataObject obj = new DataObject()
        assert obj.objProperties == []
    }
}
