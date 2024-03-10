package refactoring;
/*
 * Application.java
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 * Example: java AnnotationRemover MyJavaFile.java com.example.MyAnnotation
 *
 * @author Freya Ebba Christ 
 */
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class AdvancedControlFlowAnalyzer extends VoidVisitorAdapter<Void> {

    private Set<NameExpr> lockedVariables = new HashSet<>();

    @Override
    public void visit(TryStmt n, Void arg) {
        super.visit(n, arg);
        checkForBroadCatchClauses(n);
        checkForFinallyBlock(n);
    }

    private void checkForBroadCatchClauses(TryStmt n) {
        if (n.getCatchClauses().isEmpty()) {
            System.out.println("Warning: Try block without catch clause. Consider adding catch clauses for specific exceptions.");
            return;
        }
        n.getCatchClauses().forEach(catchClause -> {
            Parameter parameter = catchClause.getParameter();
            if (parameter == null) {
                System.out.println("Warning: Empty catch clause. Consider adding catch clauses for specific exceptions.");
            } else {
                String exceptionType = parameter.getType().asString();
                if (exceptionType.equals("Exception") || exceptionType.equals("Throwable")) {
                    System.out.println("Warning: Catching " + exceptionType + ". Consider catching more specific exceptions.");
                }
            }
        });
    }

    private void checkForFinallyBlock(TryStmt n) {
        if (!n.getFinallyBlock().isPresent()) {
            System.out.println("Warning: Try block without finally block. Consider adding a finally block for cleanup.");
        }
    }

    @Override
    public void visit(SynchronizedStmt synchronizedStmt, Void arg) {
        super.visit(synchronizedStmt, arg);
        analyzeSynchronizedBlock(synchronizedStmt);
    }

    private void analyzeSynchronizedBlock(SynchronizedStmt synchronizedStmt) {
        System.out.println("Analyzing synchronized block...");
        checkForNonFinalFieldLock(synchronizedStmt);
        checkForEmptyBlock(synchronizedStmt);
        checkForExcessiveContention(synchronizedStmt);
        checkForNestedSynchronizedBlocks(synchronizedStmt);
        analyzeLockUsage(synchronizedStmt);
    }

    private void checkForNonFinalFieldLock(SynchronizedStmt synchronizedStmt) {
        if (!isLockOnFinalField(synchronizedStmt)) {
            System.out.println("Warning: Synchronized block may lock on a non-final field. Consider using a private final field or a dedicated lock object.");
        }
    }

    private void checkForEmptyBlock(SynchronizedStmt synchronizedStmt) {
        if (isEmptySynchronizedBlock(synchronizedStmt)) {
            System.out.println("Warning: Empty synchronized block found. Ensure it is intentional.");
        }
    }

    private void checkForExcessiveContention(SynchronizedStmt synchronizedStmt) {
        if (isExcessiveContention(synchronizedStmt)) {
            System.out.println("Warning: Excessive contention detected. Consider using finer-grained locks or lock-free algorithms.");
        }
    }

    private void checkForNestedSynchronizedBlocks(SynchronizedStmt synchronizedStmt) {
        if (hasNestedSynchronizedBlocks(synchronizedStmt)) {
            System.out.println("Warning: Nested synchronized blocks found. Avoid nesting synchronized blocks as it may lead to deadlock.");
        }
    }

    private boolean isLockOnFinalField(SynchronizedStmt synchronizedStmt) {
        return synchronizedStmt.getExpression().isNameExpr()
                && ((NameExpr) synchronizedStmt.getExpression()).getNameAsString().startsWith("this");
    }

    private boolean isEmptySynchronizedBlock(SynchronizedStmt synchronizedStmt) {
        return !synchronizedStmt.getBody().isBlockStmt() || synchronizedStmt.getBody().asBlockStmt().isEmpty();
    }

    private boolean isExcessiveContention(SynchronizedStmt synchronizedStmt) {
        Optional<CompilationUnit> optionalRoot = synchronizedStmt.findCompilationUnit();
        if (optionalRoot.isPresent()) {
            CompilationUnit root = optionalRoot.get();
            long count = root.findAll(SynchronizedStmt.class).stream()
                    .filter(block -> block.getExpression().equals(synchronizedStmt.getExpression()))
                    .count();
            return count > 2;
        }
        return false;
    }

    private boolean hasNestedSynchronizedBlocks(SynchronizedStmt synchronizedStmt) {
        if (synchronizedStmt.getBody().isBlockStmt()) {
            NodeList<Statement> statements = synchronizedStmt.getBody().asBlockStmt().getStatements();
            for (Statement statement : statements) {
                if (statement.isSynchronizedStmt()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void analyzeLockUsage(SynchronizedStmt synchronizedStmt) {
        if (synchronizedStmt.getExpression().isNameExpr()) {
            NameExpr lockVariable = (NameExpr) synchronizedStmt.getExpression();
            if (lockedVariables.contains(lockVariable)) {
                System.out.println("Warning: Reentrant lock usage detected on variable '" + lockVariable.getNameAsString() + "'.");
            } else {
                lockedVariables.add(lockVariable);
                Optional<CompilationUnit> optionalRoot = synchronizedStmt.findCompilationUnit();
                if (optionalRoot.isPresent()) {
                    CompilationUnit root = optionalRoot.get();
                    root.findAll(NameExpr.class).stream()
                            .filter(expr -> expr.equals(lockVariable))
                            .forEach(expr -> {
                                if (!expr.equals(synchronizedStmt.getExpression())) {
                                    System.out.println("Warning: Lock variable '" + lockVariable.getNameAsString() + "' accessed outside synchronized block.");
                                }
                            });
                }
            }
        }
    }

    @Override
    public void visit(ForStmt n, Void arg) {
        super.visit(n, arg);

        if (checkVariableDeclaration(n) && checkIncrement(n) && !isEmptyLoop(n)) {
            System.out.println("This for-loop might be eligible for enhancement:");
            System.out.println(n);
        }

        if (!n.getCompare().isPresent()) {
            Expression compare = n.getCompare().get();

            if (compare.isBinaryExpr()) {
                String loopIndexVariable = extractLoopIndexVariable(n);
                analyzeBinaryExpression(compare.asBinaryExpr(), loopIndexVariable);
            }
        }
    }

    private String extractLoopIndexVariable(ForStmt forStmt) {
        // Check if there's exactly one initialization expression.
        if (forStmt.getInitialization().size() != 1) {
            return null; // Cannot determine the index variable if there are multiple or no initializations.
        }

        Expression init = forStmt.getInitialization().get(0);

        // Ensure the initialization is a variable declaration.
        if (!(init instanceof VariableDeclarationExpr)) {
            return null;
        }

        VariableDeclarationExpr varDeclExpr = (VariableDeclarationExpr) init;

        // Ensure there's exactly one variable being declared.
        if (varDeclExpr.getVariables().size() != 1) {
            return null; // Cannot determine the index variable if multiple variables are declared.
        }

        // Extract and return the name of the variable.
        VariableDeclarator var = varDeclExpr.getVariables().get(0);
        return var.getNameAsString();
    }

    private boolean analyzeBinaryExpression(BinaryExpr binaryExpr, String loopIndexVariable) {
        // Check if the expression directly compares an index against a length or size.
        if (isDirectComparison(binaryExpr, loopIndexVariable)) {
            return true;
        }

        // Recursively analyze left and right sides for nested binary expressions.
        Expression left = binaryExpr.getLeft();
        Expression right = binaryExpr.getRight();

        if (left.isBinaryExpr() && analyzeBinaryExpression(left.asBinaryExpr(), loopIndexVariable)) {
            return true;
        }
        if (right.isBinaryExpr() && analyzeBinaryExpression(right.asBinaryExpr(), loopIndexVariable)) {
            return true;
        }

        return false;
    }

    private boolean checkVariableDeclaration(ForStmt forStmt) {
        if (forStmt.getInitialization().size() != 1) {
            return false;
        }

        Expression initExpression = forStmt.getInitialization().get(0);
        if (!(initExpression instanceof VariableDeclarationExpr)) {
            return false;
        }

        VariableDeclarationExpr varDeclExpr = (VariableDeclarationExpr) initExpression;
        return varDeclExpr.getVariables().size() == 1;
    }

    private boolean isDirectComparison(BinaryExpr binaryExpr, String loopIndexVariable) {
        Expression left = binaryExpr.getLeft();
        Expression right = binaryExpr.getRight();

        // Check if the left or right side is a simple name that matches the loop's index variable
        boolean leftIsIndex = left.isNameExpr() && left.asNameExpr().getNameAsString().equals(loopIndexVariable);
        boolean rightIsIndex = right.isNameExpr() && right.asNameExpr().getNameAsString().equals(loopIndexVariable);

        // Check if the opposite side is a method call to 'size()' or field access to 'length'
        boolean leftIsSizeOrLength = left.isMethodCallExpr() && left.asMethodCallExpr().getNameAsString().equals("size")
                || (left.isFieldAccessExpr() && left.asFieldAccessExpr().getNameAsString().equals("length"));
        boolean rightIsSizeOrLength = right.isMethodCallExpr() && right.asMethodCallExpr().getNameAsString().equals("size")
                || (right.isFieldAccessExpr() && right.asFieldAccessExpr().getNameAsString().equals("length"));

        return (leftIsIndex && rightIsSizeOrLength) || (rightIsIndex && leftIsSizeOrLength);
    }

    private boolean isArrayOrCollectionIteration(ForStmt forStmt) {
        if (!forStmt.getCompare().isPresent()) {
            return false;
        }

        Expression compare = forStmt.getCompare().get();
        // Assuming a simple binary expression comparison: i < array.length or i < collection.size()
        if (!(compare instanceof BinaryExpr)) {
            return false;
        }
        BinaryExpr binaryExpr = (BinaryExpr) compare;

        // Check if the right side of the comparison is a method call (e.g., size(), length)
        if (!(binaryExpr.getRight() instanceof MethodCallExpr)) {
            return false;
        }
        MethodCallExpr methodCallExpr = (MethodCallExpr) binaryExpr.getRight();

        String methodName = methodCallExpr.getNameAsString();
        if (!(methodName.equals("length") || methodName.equals("size"))) {
            return false;
        }

        try {
            // Perform type resolution on the method call's scope to determine the type
            ResolvedType resolvedType = methodCallExpr.getScope().get().calculateResolvedType();
            // Check if the type is an array or implements java.util.Collection
            if (resolvedType.isArray()) {
                return true;
            }
            if (resolvedType.isReferenceType()) {
                // Check for Collection type
                if (resolvedType.isReferenceType()) {
                    // Check for Collection type
                    return isCollectionType(resolvedType);
                }
            }
        } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
            System.err.println("Error resolving type: " + e.getMessage());
        }
        return false;
    }

    private boolean isCollectionType(ResolvedType resolvedType) {
        if (!resolvedType.isReferenceType()) {
            return false;
        }

        // Obtain the list of all ancestors of the resolved type, which includes all superclasses and interfaces.
        try {
            for (ResolvedReferenceType ancestor : resolvedType.asReferenceType().getAllAncestors()) {
                String ancestorName = ancestor.getQualifiedName();
                if (ancestorName.equals("java.util.Collection")) {
                    return true;
                }
            }
        } catch (UnsolvedSymbolException e) {
            System.err.println("Could not resolve ancestor types for: " + resolvedType.describe());
        }

        return false;
    }

    private boolean checkIncrement(ForStmt forStmt) {
        if (forStmt.getUpdate().size() != 1) {
            return false;
        }

        Expression updateExpr = forStmt.getUpdate().get(0);

        if (updateExpr instanceof UnaryExpr) {
            UnaryExpr unaryExpr = (UnaryExpr) updateExpr;
            return unaryExpr.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                    || unaryExpr.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT;
        } else if (updateExpr instanceof AssignExpr) {
            AssignExpr assignExpr = (AssignExpr) updateExpr;
            if (assignExpr.getValue() instanceof BinaryExpr) {
                BinaryExpr binaryExpr = (BinaryExpr) assignExpr.getValue();
                return (binaryExpr.getOperator() == BinaryExpr.Operator.PLUS
                        && binaryExpr.getRight().isIntegerLiteralExpr()
                        && binaryExpr.getRight().asIntegerLiteralExpr().getValue().equals("1"))
                        || (binaryExpr.getOperator() == BinaryExpr.Operator.MINUS
                        && binaryExpr.getRight().isIntegerLiteralExpr()
                        && binaryExpr.getRight().asIntegerLiteralExpr().getValue().equals("1"));
            }
        } else if (updateExpr instanceof BinaryExpr) {
            BinaryExpr binaryExpr = (BinaryExpr) updateExpr;
            return ((binaryExpr.getOperator() == BinaryExpr.Operator.PLUS
                    && binaryExpr.getRight().isIntegerLiteralExpr()
                    && binaryExpr.getRight().asIntegerLiteralExpr().getValue().equals("1"))
                    || (binaryExpr.getOperator() == BinaryExpr.Operator.MINUS
                    && binaryExpr.getRight().isIntegerLiteralExpr()
                    && binaryExpr.getRight().asIntegerLiteralExpr().getValue().equals("1")));
        }

        return false;
    }

    @Override
    public void visit(WhileStmt n, Void arg) {
        super.visit(n, arg);
        analyzeWhileLoop(n);
    }

    private void analyzeWhileLoop(WhileStmt whileStmt) {
        System.out.println("Analyzing while-loop...");
        checkForEmptyWhileLoop(whileStmt);
        checkForNullCheckLoop(whileStmt);
    }

    private void checkForEmptyWhileLoop(WhileStmt whileStmt) {
        if (isEmptyWhileLoop(whileStmt)) {
            System.out.println("Warning: Empty while-loop found. Ensure it is intentional.");
        }
    }

    private boolean isEmptyWhileLoop(WhileStmt whileStmt) {
        return !whileStmt.getBody().isBlockStmt() || whileStmt.getBody().asBlockStmt().isEmpty();
    }

    private void checkForNullCheckLoop(WhileStmt whileStmt) {
        Expression condition = whileStmt.getCondition();
        if (condition.isBinaryExpr()) {
            BinaryExpr binaryExpr = condition.asBinaryExpr();
            if (binaryExpr.getOperator() == BinaryExpr.Operator.NOT_EQUALS && binaryExpr.getRight() instanceof NullLiteralExpr) {
                System.out.println("Warning: Infinite loop detected due to null check. Ensure there's a break or return statement inside the loop.");
            }
        }
    }

    private boolean isEmptyLoop(ForStmt forStmt) {
        Statement body = forStmt.getBody();
        if (body instanceof BlockStmt) {
            BlockStmt block = (BlockStmt) body;
            return block.getStatements().isEmpty();
        }
        // Consider non-block bodies (e.g., single statements) as non-empty.
        return false;
   }
}
