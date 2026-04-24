package com.toastcoders.vmware.yavijava.migration

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.MethodDeclaration
import org.junit.Test

class MoMemberClassifierTest {

    private MethodDeclaration parseMethod(String src) {
        def cu = StaticJavaParser.parse(
            "class X { ${src} }")
        return cu.getType(0).methods[0]
    }

    @Test
    void testGetCurrentPropertyIsAutoGen() {
        def m = parseMethod('public String getName() { return (String) getCurrentProperty("name"); }')
        assert new MoMemberClassifier().isAutoGeneratable(m)
    }

    @Test
    void testGetManagedObjectIsAutoGen() {
        def m = parseMethod('public ManagedEntity getParent() { return (ManagedEntity) this.getManagedObject("parent"); }')
        assert new MoMemberClassifier().isAutoGeneratable(m)
    }

    @Test
    void testGetTasksHelperIsAutoGen() {
        def m = parseMethod('public Task[] getRecentTask() { return getTasks("recentTask"); }')
        assert new MoMemberClassifier().isAutoGeneratable(m)
    }

    @Test
    void testGetVmsHelperIsAutoGen() {
        def m = parseMethod('public VirtualMachine[] getVm() { return getVms("vm"); }')
        assert new MoMemberClassifier().isAutoGeneratable(m)
    }

    @Test
    void testCustomHelperGetterIsNotAutoGen() {
        def m = parseMethod('public Task[] getRecentTasks() { return getTasks("recentTask"); }')
        assert new MoMemberClassifier().isAutoGeneratable(m)
    }

    @Test
    void testNullCoalescingGetterIsNotAutoGen() {
        def m = parseMethod('''
            public boolean getAlarmActionEabled() {
                Boolean aae = (Boolean) getCurrentProperty("alarmActionsEnabled");
                return aae == null ? false : aae.booleanValue();
            }
        ''')
        assert !new MoMemberClassifier().isAutoGeneratable(m)
    }

    @Test
    void testVoidVimServiceCallIsAutoGen() {
        def m = parseMethod('public void reload() throws RuntimeFault, RemoteException { getVimService().reload(getMOR()); }')
        assert new MoMemberClassifier().isAutoGeneratable(m)
    }

    @Test
    void testTaskReturningVimServiceIsAutoGen() {
        def m = parseMethod('''
            public Task destroy_Task() throws VimFault, RuntimeFault, RemoteException {
                ManagedObjectReference taskMor = getVimService().destroy_Task(getMOR());
                return new Task(getServerConnection(), taskMor);
            }
        ''')
        assert new MoMemberClassifier().isAutoGeneratable(m)
    }

    @Test
    void testVimServiceWithArgsIsAutoGen() {
        def m = parseMethod('''
            public Task rename_Task(String name) throws InvalidName, DuplicateName, RuntimeFault, RemoteException {
                ManagedObjectReference taskMor = getVimService().rename_Task(getMOR(), name);
                return new Task(getServerConnection(), taskMor);
            }
        ''')
        assert new MoMemberClassifier().isAutoGeneratable(m)
    }

    @Test
    void testCustomLogicMethodIsNotAutoGen() {
        def m = parseMethod('''
            public void doSomething() {
                int x = 5;
                System.out.println(x);
            }
        ''')
        assert !new MoMemberClassifier().isAutoGeneratable(m)
    }

    @Test
    void testConstructorIsAlwaysCustom() {
        def cu = StaticJavaParser.parse(
            'class X { public X(int a) { this.a = a; } int a; }')
        def ctor = cu.getType(0).constructors[0]
        assert !new MoMemberClassifier().isAutoGeneratable(ctor)
    }
}
