package com.toastcoders.vmware.yavijava.migration

import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.CastExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement

class MoMemberClassifier {

    private static final Set<String> TYPED_HELPERS = [
        "getTasks", "getDatastores", "getHosts", "getVms", "getNetworks",
        "getResourcePools", "getScheduledTasks", "getViews", "getFilter"
    ] as Set

    boolean isAutoGeneratable(BodyDeclaration<?> member) {
        if (!(member instanceof MethodDeclaration)) return false
        MethodDeclaration m = member as MethodDeclaration
        if (!m.getBody().isPresent()) return false
        def stmts = m.getBody().get().getStatements()
        if (stmts.size() == 1) return matchesSingleStatementShape(stmts[0])
        if (stmts.size() == 2) return matchesTaskReturningShape(stmts)
        return false
    }

    private boolean matchesSingleStatementShape(Statement s) {
        if (s instanceof ReturnStmt) {
            def expr = (s as ReturnStmt).expression.orElse(null)
            if (expr == null) return false
            if (expr instanceof CastExpr) {
                def inner = (expr as CastExpr).expression
                if (inner instanceof MethodCallExpr) {
                    return matchesPropertyOrManagedObject(inner as MethodCallExpr)
                }
                return false
            }
            if (expr instanceof MethodCallExpr) {
                def call = expr as MethodCallExpr
                if (TYPED_HELPERS.contains(call.nameAsString) &&
                    call.arguments.size() == 1 &&
                    call.arguments[0] instanceof StringLiteralExpr) {
                    return true
                }
            }
            return false
        }
        if (s instanceof ExpressionStmt) {
            def expr = (s as ExpressionStmt).expression
            if (expr instanceof MethodCallExpr) {
                return matchesGetVimServiceCall(expr as MethodCallExpr)
            }
        }
        return false
    }

    private boolean matchesPropertyOrManagedObject(MethodCallExpr call) {
        if (call.arguments.size() != 1) return false
        if (!(call.arguments[0] instanceof StringLiteralExpr)) return false
        String name = call.nameAsString
        return name == "getCurrentProperty" || name == "getManagedObject" || name == "getManagedObjects"
    }

    private boolean matchesTaskReturningShape(List<Statement> stmts) {
        Statement first = stmts[0]
        Statement second = stmts[1]
        if (!(second instanceof ReturnStmt)) return false
        def returnExpr = (second as ReturnStmt).expression.orElse(null)
        if (!(returnExpr instanceof ObjectCreationExpr)) return false
        def creation = returnExpr as ObjectCreationExpr
        if (creation.arguments.size() != 2) return false
        def arg0 = creation.arguments[0]
        if (!(arg0 instanceof MethodCallExpr) || (arg0 as MethodCallExpr).nameAsString != "getServerConnection") return false
        if (first.toString().contains("getVimService()") && first.toString().contains("getMOR()")) return true
        return false
    }

    private boolean matchesGetVimServiceCall(MethodCallExpr call) {
        if (!call.scope.isPresent()) return false
        def scope = call.scope.get()
        if (!(scope instanceof MethodCallExpr)) return false
        if ((scope as MethodCallExpr).nameAsString != "getVimService") return false
        if (call.arguments.isEmpty()) return false
        def first = call.arguments[0]
        if (!(first instanceof MethodCallExpr) || (first as MethodCallExpr).nameAsString != "getMOR") return false
        return true
    }
}
