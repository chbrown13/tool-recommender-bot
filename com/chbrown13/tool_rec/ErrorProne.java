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
	"{s}      <arg>-Xplugin:ErrorProne -XepAllErrorsAsWarnings -Xep:AndroidJdkLibsChecker:ERROR -Xep:AssistedInjectAndInjectOnSameConstructor:ERROR -Xep:AutoFactoryAtInject:ERROR -Xep:ClassName:ERROR -Xep:ComparisonContractViolated:ERROR -Xep:DepAnn:ERROR -Xep:DivZero:ERROR -Xep:EmptyIf:ERROR -Xep:FuzzyEqualsShouldNotBeUsedInEqualsMethod:ERROR -Xep:InjectInvalidTargetingOnScopingAnnotation:ERROR -Xep:InjectMoreThanOneQualifier:ERROR -Xep:InjectScopeAnnotationOnInterfaceOrAbstractClass:ERROR -Xep:InjectScopeOrQualifierAnnotationRetention:ERROR -Xep:InjectedConstructorAnnotations:ERROR -Xep:InsecureCryptoUsage:ERROR -Xep:IterablePathParameter:ERROR -Xep:JMockTestWithoutRunWithOrRuleAnnotation:ERROR -Xep:Java7ApiChecker:ERROR -Xep:JavaxInjectOnFinalField:ERROR -Xep:LockMethodChecker:ERROR -Xep:LongLiteralLowerCaseSuffix:ERROR -Xep:NoAllocation:ERROR -Xep:NumericEquality:ERROR -Xep:ParameterPackage:ERROR -Xep:RestrictTo:ERROR -Xep:StaticOrDefaultInterfaceMethod:ERROR -Xep:UnlockMethod:ERROR -Xep:AnnotateFormatMethod:ERROR -Xep:AnnotationPosition:ERROR -Xep:AssertFalse:ERROR -Xep:AssistedInjectAndInjectOnConstructors:ERROR -Xep:BinderIdentityRestoredDangerously:ERROR -Xep:BindingToUnqualifiedCommonType:ERROR -Xep:ConstructorInvokesOverridable:ERROR -Xep:ConstructorLeaksThis:ERROR -Xep:EmptyTopLevelDeclaration:ERROR -Xep:EqualsBrokenForNull:ERROR -Xep:ExpectedExceptionChecker:ERROR -Xep:FunctionalInterfaceClash:ERROR -Xep:HardCodedSdCardPath:ERROR -Xep:InconsistentOverloads:ERROR -Xep:InvalidParam:ERROR -Xep:InvalidTag:ERROR -Xep:InvalidThrows:ERROR -Xep:MissingDefault:ERROR -Xep:MutableMethodReturnType:ERROR -Xep:NoFunctionalReturnType:ERROR -Xep:NonCanonicalStaticMemberImport:ERROR -Xep:NullableDereference:ERROR -Xep:PrimitiveArrayPassedToVarargsMethod:ERROR -Xep:ProtosAsKeyOfSetOrMap:ERROR -Xep:ProvidesFix:ERROR -Xep:QualifierWithTypeUse:ERROR -Xep:RedundantThrows:ERROR -Xep:ReturnFromVoid:ERROR -Xep:StaticQualifiedUsingExpression:ERROR -Xep:StringEquality:ERROR -Xep:SystemExitOutsideMain:ERROR -Xep:TestExceptionChecker:ERROR -Xep:UnnecessaryDefaultInEnumSwitch:ERROR -Xep:Unused:ERROR -Xep:UnusedException:ERROR -Xep:Var:ERROR -Xep:BooleanParameter:ERROR -Xep:ClassNamedLikeTypeParameter:ERROR -Xep:ConstantField:ERROR -Xep:EmptySetMultibindingContributions:ERROR -Xep:ExpectedExceptionRefactoring:ERROR -Xep:FieldCanBeFinal:ERROR -Xep:FieldMissingNullable:ERROR -Xep:ImmutableRefactoring:ERROR -Xep:LambdaFunctionalInterface:ERROR -Xep:MethodCanBeStatic:ERROR -Xep:MixedArrayDimensions:ERROR -Xep:MultiVariableDeclaration:ERROR -Xep:MultipleTopLevelClasses:ERROR -Xep:MultipleUnaryOperatorsInMethodCall:ERROR -Xep:PackageLocation:ERROR -Xep:ParameterComment:ERROR -Xep:ParameterNotNullable:ERROR -Xep:PrivateConstructorForNoninstantiableModule:ERROR -Xep:PrivateConstructorForUtilityClass:ERROR -Xep:RemoveUnusedImports:ERROR -Xep:ReturnMissingNullable:ERROR -Xep:ScopeOnModule:ERROR -Xep:SwitchDefault:ERROR -Xep:TestExceptionRefactoring:ERROR -Xep:ThrowsUncheckedException:ERROR -Xep:TryFailRefactoring:ERROR -Xep:TypeParameterNaming:ERROR -Xep:UngroupedOverloads:ERROR -Xep:UnnecessarySetDefault:ERROR -Xep:UnnecessaryStaticImport:ERROR -Xep:UseBinds:ERROR -Xep:WildcardImport:ERROR</arg>\n"+
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

	public String getProfile() {
		return this.PROFILE;
	}
	
	public String getProperty() {
		return this.PROPERTY;
	}
	
	public static String getBody() {
		return REC.replace("{code}", CODE).replace("{error}", ERROR);
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
		return errors;
	}
}

