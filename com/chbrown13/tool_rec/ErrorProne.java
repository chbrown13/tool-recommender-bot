package com.chbrown13.tool_rec;

//import com.google.errorprone.*;
//import com.sun.tools.javac.main.Main.Result;
import java.io.*;
import java.util.*;
import java.util.regex.*;


/**
 * The ErrorProne class contains methods concerning the ErrorProne static analysis tool and an object storing information for a bug reported.
 */
public class ErrorProne extends Tool {
	
	private final String MAVEN = "{s}<plugin>\n"+
	"{s}  <groupId>org.apache.maven.plugins</groupId>\n"+
	"{s}  <artifactId>maven-compiler-plugin</artifactId>\n"+
	"{s}  <version>3.5.1</version>\n"+
	"{s}  <configuration>\n"+
	"{s}    <source>8</source>\n"+
	"{s}    <target>8</target>\n"+
	"{s}    <showWarnings>true</showWarnings>\n"+
	"{s}    <compilerArgs>\n"+
	"{s}      <arg>-XDcompilePolicy=simple</arg>\n"+
	"{s}      <arg>-Xplugin:ErrorProne -XepAllErrorsAsWarnings -Xep:AndroidJdkLibsChecker:WARN -Xep:AssistedInjectAndInjectOnSameConstructor:WARN -Xep:AutoFactoryAtInject:WARN -Xep:ClassName:WARN -Xep:ComparisonContractViolated:WARN -Xep:DepAnn:WARN -Xep:DivZero:WARN -Xep:EmptyIf:WARN -Xep:FuzzyEqualsShouldNotBeUsedInEqualsMethod:WARN -Xep:InjectInvalidTargetingOnScopingAnnotation:WARN -Xep:InjectMoreThanOneQualifier:WARN -Xep:InjectScopeAnnotationOnInterfaceOrAbstractClass:WARN -Xep:InjectScopeOrQualifierAnnotationRetention:WARN -Xep:InjectedConstructorAnnotations:WARN -Xep:InsecureCryptoUsage:WARN -Xep:IterablePathParameter:WARN -Xep:JMockTestWithoutRunWithOrRuleAnnotation:WARN -Xep:Java7ApiChecker:WARN -Xep:JavaxInjectOnFinalField:WARN -Xep:LockMethodChecker:WARN -Xep:LongLiteralLowerCaseSuffix:WARN -Xep:NoAllocation:WARN -Xep:NumericEquality:WARN -Xep:ParameterPackage:WARN -Xep:RestrictTo:WARN -Xep:StaticOrDefaultInterfaceMethod:WARN -Xep:UnlockMethod:WARN -Xep:AnnotateFormatMethod:WARN -Xep:AnnotationPosition:WARN -Xep:AssertFalse:WARN -Xep:AssistedInjectAndInjectOnConstructors:WARN -Xep:BinderIdentityRestoredDangerously:WARN -Xep:BindingToUnqualifiedCommonType:WARN -Xep:ConstructorInvokesOverridable:WARN -Xep:ConstructorLeaksThis:WARN -Xep:EmptyTopLevelDeclaration:WARN -Xep:EqualsBrokenForNull:WARN -Xep:ExpectedExceptionChecker:WARN -Xep:FunctionalInterfaceClash:WARN -Xep:HardCodedSdCardPath:WARN -Xep:InconsistentOverloads:WARN -Xep:InvalidParam:WARN -Xep:InvalidTag:WARN -Xep:InvalidThrows:WARN -Xep:MissingDefault:WARN -Xep:MutableMethodReturnType:WARN -Xep:NoFunctionalReturnType:WARN -Xep:NonCanonicalStaticMemberImport:WARN -Xep:NullableDereference:WARN -Xep:PrimitiveArrayPassedToVarargsMethod:WARN -Xep:ProtosAsKeyOfSetOrMap:WARN -Xep:ProvidesFix:WARN -Xep:QualifierWithTypeUse:WARN -Xep:RedundantThrows:WARN -Xep:ReturnFromVoid:WARN -Xep:StaticQualifiedUsingExpression:WARN -Xep:StringEquality:WARN -Xep:SystemExitOutsideMain:WARN -Xep:TestExceptionChecker:WARN -Xep:UnnecessaryDefaultInEnumSwitch:WARN -Xep:Unused:WARN -Xep:UnusedException:WARN -Xep:Var:WARN -Xep:BooleanParameter:WARN -Xep:ClassNamedLikeTypeParameter:WARN -Xep:ConstantField:WARN -Xep:EmptySetMultibindingContributions:WARN -Xep:ExpectedExceptionRefactoring:WARN -Xep:FieldCanBeFinal:WARN -Xep:FieldMissingNullable:WARN -Xep:ImmutableRefactoring:WARN -Xep:LambdaFunctionalInterface:WARN -Xep:MethodCanBeStatic:WARN -Xep:MixedArrayDimensions:WARN -Xep:MultiVariableDeclaration:WARN -Xep:MultipleTopLevelClasses:WARN -Xep:MultipleUnaryOperatorsInMethodCall:WARN -Xep:PackageLocation:WARN-Xep:ParameterComment:WARN -Xep:ParameterNotNullable:WARN -Xep:PrivateConstructorForNoninstantiableModule:WARN -Xep:PrivateConstructorForUtilityClass:WARN -Xep:RemoveUnusedImports:WARN -Xep:ReturnMissingNullable:WARN -Xep:ScopeOnModule:WARN -Xep:SwitchDefault:WARN -Xep:TestExceptionRefactoring:WARN -Xep:ThrowsUncheckedException:WARN -Xep:TryFailRefactoring:WARN -Xep:TypeParameterNaming:WARN -Xep:UngroupedOverloads:WARN -Xep:UnnecessarySetDefault:WARN -Xep:UnnecessaryStaticImport:WARN -Xep:UseBinds:WARN -Xep:WildcardImport:WARN</arg>\n"+
	"{s}    </compilerArgs>\n"+
	"{s}    <annotationProcessorPaths>\n"+
	"{s}      <path>\n"+
	"{s}        <groupId>com.google.errorprone</groupId>\n"+
	"{s}        <artifactId>error_prone_core</artifactId>\n"+
	"{s}        <version>2.3.2</version>\n"+
	"{s}      </path>\n"+
	"{s}    </annotationProcessorPaths>\n"+
	"{s}  </configuration>\n"+
	"{s}</plugin>\n";

	private final String PROFILE = "{s}<profile>\n"+
	"{s}  <id>jdk8</id>\n"+
	"{s}  <activation>\n"+
	"{s}    <jdk>1.8</jdk>\n"+
	"{s}  </activation>\n"+
	"{s}  <build>\n"+
	"{s}    <plugins>\n"+
	"{s}      <plugin>\n"+
	"{s}        <groupId>org.apache.maven.plugins</groupId>\n"+
	"{s}        <artifactId>maven-compiler-plugin</artifactId>\n"+
	"{s}        <configuration>\n"+
	"{s}          <fork>true</fork>\n"+
	"{s}          <compilerArgs combine.children=\"append\">\n"+
	"{s}            <arg>-J-Xbootclasspath/p:${settings.localRepository}/com/google/errorprone/javac/${javac.version}/javac-${javac.version}.jar</arg>\n"+
	"{s}          </compilerArgs>\n"+
	"{s}        </configuration>\n"+
	"{s}      </plugin>\n"+
	"{s}    </plugins>\n"+
	"{s}  </build>\n"+
	"{s}</profile>\n";

	private final String PROPERTY = "{s}<javac.version>9+181-r4173-1</javac.version>\n";

	private final static String REC = "Looks like you're not using any error-checking in your Java build. This pull requests adds a static analysis tool, [Error Prone](http://errorprone.info), created by Google to find common errors in Java code. For example, running ```mvn compile``` on the following code:\n" +
	"```java\n{code}\n```\n" +
	"would identify this error:\n" +
	"```\n{error}\n```\n" +
	"If you think you might want to try out this plugin, you can just merge this pull request. Please feel free to add any comments below explaining why you did or did not find this recommendation useful.";
	
	private final static String CODE = "public boolean validate(String s) {\n" +
	"	return s == this.username;\n" +
	"}";

	private final static String ERROR = "[ERROR] src/main/java/HelloWorld.java:[17,17] error: [StringEquality] String comparison using reference equality instead of value equality\n" +
	"[ERROR]     (see https://errorprone.info/bugpattern/StringEquality)";

	public ErrorProne() {
		super("Error Prone", "static analysis tool", "http://errorprone.info");
	}

	/**
	 * Returns the Error Prone maven plugin for build
	 */
	@Override
	public String getPlugin() {
		return this.MAVEN;
	}

	@Override
	public String getProfile() {
		return this.PROFILE;
	}
	
	@Override
	public String getProperty() {
		return this.PROPERTY;
	}
	
	@Override
	public String getRec() {
		return this.REC.replace("{code}", CODE).replace("{error}", ERROR);
	}

	/**
	 * Parses output from ErrorProne static analysis of code and creates Errors.
	 *
	 * @param log    ErrorProne output
	 * 
	 * @return       List of Error objects
	 */
	@Override
	public List<Error> parseOutput(String log) {
		List<Error> errors = new ArrayList<Error>();
		if (log == null || log.isEmpty()) {
			return null;
		} else if (log.contains("java.lang.RuntimeException")) {
			System.out.println("Maven build error");
			return null;
		} else if (log.contains("[INFO] BUILD SUCCESS")) {
			System.out.println("No errors.");
			return errors;
		}
		String regex = "^[\\[ERROR\\]\\s]*/[(/\\w\\W)]+.java:\\[*\\d+(,|:)\\d+(:|\\])\\s(error:|warning:|\\[\\w+\\])";
		Pattern pattern = Pattern.compile(regex);
		Pattern err= Pattern.compile("\\[\\w+\\]");
		Error temp = null;
		String path, file, loc, offset, error, msg, info = "";
		for (String line: log.split("\n")) {
			Matcher m = pattern.matcher(line);
			if (line.startsWith("[INFO] ")) {
				if (temp != null && !errors.contains(temp)) { 
					temp.setLog(info);
					errors.add(temp); 
					temp = null;
				}
				continue;
			/*} else if (line.contains("-> [Help 1]") {
				if (temp != null && !errors.contains(temp)) { 
					temp.setLog(info);
					errors.add(temp); 
					break;
				}*/
			} else if (m.lookingAt()) {
				if (temp != null && !errors.contains(temp)) { 
					temp.setLog(info);
					errors.add(temp); 
					temp = null;
				}
				if (line.startsWith("[")) {
					line = line.substring(line.indexOf("/"));
				}
				path = file = loc = error = msg = info = "";
				path = line.substring(0, line.indexOf(":"));
				file = path.substring(path.lastIndexOf("/")+1);
				loc = line.substring(line.indexOf(":")).replaceAll("[^\\d]+", " ").split(" ")[1];
				offset = line.substring(line.indexOf(":")).replaceAll("[^\\d]+", " ").split(" ")[2];
				Matcher e = err.matcher(line.substring(line.indexOf(":")));
				if (e.find()) {
					error = e.group();
				} else {
					if (line.contains("warning: ")) {
						error = "[WARNING]";
					} else if (line.contains("error: ")) {
						error = "[ERROR]";
					}
				}
				if (error.equals("[WARNING]")) {
					msg = line.substring(line.indexOf("warning:") + 9);
				} else if (error.equals("[ERROR]")) {
					msg = line.substring(line.indexOf("error:") + 7);
				} else {
					msg = line.substring(line.indexOf(error) + error.length() + 1);
				}
				info = "";
				temp = new Error(path, file, loc, offset, error, msg, "");
			} else if (temp != null) {
				info += line + "\n";
			}
		}
		if (errors.isEmpty()) { // after parsing file
			return null;
		}
		return errors;
	}
}

