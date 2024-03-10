package nullcheckparser.example;

import nullcheckparser.NullCheckPerformed;
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
public class ModifiedYourJavaFile {

    @NullCheckPerformed
    public void exampleMethod(String param) {
        // Null check present
        if (param == null) {
            System.out.println("Parameter is null");
        }
    }

    @NullCheckPerformed
    public void anotherExampleMethod(String anotherParam) {
        // Null check present
        if (anotherParam != null) {
            System.out.println("Another parameter is not null");
        }
    }
}
