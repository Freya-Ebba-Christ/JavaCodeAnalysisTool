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
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.*;

public class NullCheckMethodVisitor extends VoidVisitorAdapter<Void> {

    private final Map<String, List<NullCheckInfo>> capturedLogic = new HashMap<>();

    @Override
    public void visit(MethodDeclaration n, Void arg) {
        super.visit(n, arg);
        n.getAnnotationByName("NullCheckPerformed").ifPresent(annotation -> {
            n.getBody().ifPresent(body -> {
                body.findAll(IfStmt.class).forEach(stmt -> processStatement(stmt, n.getSignature().asString(), "IfStatement"));
                body.findAll(SwitchStmt.class).forEach(stmt -> processStatement(stmt, n.getSignature().asString(), "SwitchStatement"));
                // Consider extending to other statement types as necessary
            });
        });
    }

    private void processStatement(Statement statement, String methodSignature, String statementType) {
        statement.findAll(Expression.class).forEach(expr -> analyzeExpression(expr, statement, methodSignature, statementType));
    }

    private void analyzeExpression(Expression expression, Statement statement, String methodSignature, String statementType) {
        expression.getBegin().ifPresent(position -> {
            toNullCheckExpr(expression).ifPresent(nullCheck -> {
                String variable = nullCheck.getChecked().toString();
                int line = position.line; // Correctly access the line number
                capturedLogic.computeIfAbsent(methodSignature, k -> new ArrayList<>())
                        .add(new NullCheckInfo(statementType, statement.toString(), nullCheck.getOperator().toString(), variable, "null", line));
            });
        });

        // Recursively analyze child nodes for expressions that could contain further expressions
        expression.getChildNodes().forEach(child -> {
            if (child instanceof Expression) {
                analyzeExpression((Expression) child, statement, methodSignature, statementType);
            }
        });
    }

    private Optional<NullCheckExpr> toNullCheckExpr(Expression expr) {
        if (expr instanceof BinaryExpr) {
            BinaryExpr binaryExpr = (BinaryExpr) expr;
            if ((binaryExpr.getOperator() == BinaryExpr.Operator.EQUALS || binaryExpr.getOperator() == BinaryExpr.Operator.NOT_EQUALS)
                    && (binaryExpr.getRight().isNullLiteralExpr() || binaryExpr.getLeft().isNullLiteralExpr())) {
                Expression checkedExpression = binaryExpr.getRight().isNullLiteralExpr() ? binaryExpr.getLeft() : binaryExpr.getRight();
                return Optional.of(new NullCheckExpr(binaryExpr.getOperator(), checkedExpression));
            }
        }
        return Optional.empty();
    }

    public Map<String, List<NullCheckInfo>> getCapturedLogic() {
        return capturedLogic;
    }

    static class NullCheckExpr {

        final BinaryExpr.Operator operator;
        final Expression checked;

        NullCheckExpr(BinaryExpr.Operator operator, Expression checked) {
            this.operator = operator;
            this.checked = checked;
        }

        public BinaryExpr.Operator getOperator() {
            return operator;
        }

        public Expression getChecked() {
            return checked;
        }
    }

    static class NullCheckInfo {

        final String context;
        final String statement;
        final String checkType;
        final String variable;
        final String comparedAgainst;
        final int line;

        NullCheckInfo(String context, String statement, String checkType, String variable, String comparedAgainst, int line) {
            this.context = context;
            this.statement = statement;
            this.checkType = checkType;
            this.variable = variable;
            this.comparedAgainst = comparedAgainst;
            this.line = line;
        }
    }
}
