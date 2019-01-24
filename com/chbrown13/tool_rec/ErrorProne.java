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
	
	private final String RUN_CMD = "java -Xbootclasspath/p:error_prone_ant-2.1.0.jar com.google.errorprone.ErrorProneCompiler {file}";	
	private final String MAVEN = "<plugin>\n"+
	"<groupId>org.apache.maven.plugins</groupId>\n"+
	"<artifactId>maven-compiler-plugin</artifactId>\n"+
	"<version>3.5.1</version>\n"+
	"<configuration>\n"+
	"	<source>8</source>\n"+
	"	<target>8</target>\n"+
	"	<showWarnings>true</showWarnings>\n"+
	"	<compilerArgs>\n"+
	"		<arg>-XDcompilePolicy=simple</arg>\n"+
	"		<arg>-Xplugin:ErrorProne -XepAllErrorsAsWarnings -Xep:AndroidJdkLibsChecker:ERROR -Xep:AssistedInjectAndInjectOnSameConstructor:ERROR -Xep:AutoFactoryAtInject:ERROR -Xep:ClassName:ERROR -Xep:ComparisonContractViolated:ERROR -Xep:DepAnn:ERROR -Xep:DivZero:ERROR -Xep:EmptyIf:ERROR -Xep:FuzzyEqualsShouldNotBeUsedInEqualsMethod:ERROR -Xep:InjectInvalidTargetingOnScopingAnnotation:ERROR -Xep:InjectMoreThanOneQualifier:ERROR -Xep:InjectScopeAnnotationOnInterfaceOrAbstractClass:ERROR -Xep:InjectScopeOrQualifierAnnotationRetention:ERROR -Xep:InjectedConstructorAnnotations:ERROR -Xep:InsecureCryptoUsage:ERROR -Xep:IterablePathParameter:ERROR -Xep:JMockTestWithoutRunWithOrRuleAnnotation:ERROR -Xep:Java7ApiChecker:ERROR -Xep:JavaxInjectOnFinalField:ERROR -Xep:LockMethodChecker:ERROR -Xep:LongLiteralLowerCaseSuffix:ERROR -Xep:NoAllocation:ERROR -Xep:NumericEquality:ERROR -Xep:ParameterPackage:ERROR -Xep:RestrictTo:ERROR -Xep:StaticOrDefaultInterfaceMethod:ERROR -Xep:UnlockMethod:ERROR -Xep:AnnotateFormatMethod:ERROR -Xep:AnnotationPosition:ERROR -Xep:AssertFalse:ERROR -Xep:AssistedInjectAndInjectOnConstructors:ERROR -Xep:BinderIdentityRestoredDangerously:ERROR -Xep:BindingToUnqualifiedCommonType:ERROR -Xep:ConstructorInvokesOverridable:ERROR -Xep:ConstructorLeaksThis:ERROR -Xep:EmptyTopLevelDeclaration:ERROR -Xep:EqualsBrokenForNull:ERROR -Xep:ExpectedExceptionChecker:ERROR -Xep:FunctionalInterfaceClash:ERROR -Xep:HardCodedSdCardPath:ERROR -Xep:InconsistentOverloads:ERROR -Xep:InvalidParam:ERROR -Xep:InvalidTag:ERROR -Xep:InvalidThrows:ERROR -Xep:MissingDefault:ERROR -Xep:MutableMethodReturnType:ERROR -Xep:NoFunctionalReturnType:ERROR -Xep:NonCanonicalStaticMemberImport:ERROR -Xep:NullableDereference:ERROR -Xep:PrimitiveArrayPassedToVarargsMethod:ERROR -Xep:ProtosAsKeyOfSetOrMap:ERROR -Xep:ProvidesFix:ERROR -Xep:QualifierWithTypeUse:ERROR -Xep:RedundantThrows:ERROR -Xep:ReturnFromVoid:ERROR -Xep:StaticQualifiedUsingExpression:ERROR -Xep:StringEquality:ERROR -Xep:SystemExitOutsideMain:ERROR -Xep:TestExceptionChecker:ERROR -Xep:UnnecessaryDefaultInEnumSwitch:ERROR -Xep:Unused:ERROR -Xep:UnusedException:ERROR -Xep:Var:ERROR -Xep:BooleanParameter:ERROR -Xep:ClassNamedLikeTypeParameter:ERROR -Xep:ConstantField:ERROR -Xep:EmptySetMultibindingContributions:ERROR -Xep:ExpectedExceptionRefactoring:ERROR -Xep:FieldCanBeFinal:ERROR -Xep:FieldMissingNullable:ERROR -Xep:ImmutableRefactoring:ERROR -Xep:LambdaFunctionalInterface:ERROR -Xep:MethodCanBeStatic:ERROR -Xep:MixedArrayDimensions:ERROR -Xep:MultiVariableDeclaration:ERROR -Xep:MultipleTopLevelClasses:ERROR -Xep:MultipleUnaryOperatorsInMethodCall:ERROR -Xep:PackageLocation:ERROR -Xep:ParameterComment:ERROR -Xep:ParameterNotNullable:ERROR -Xep:PrivateConstructorForNoninstantiableModule:ERROR -Xep:PrivateConstructorForUtilityClass:ERROR -Xep:RemoveUnusedImports:ERROR -Xep:ReturnMissingNullable:ERROR -Xep:ScopeOnModule:ERROR -Xep:SwitchDefault:ERROR -Xep:TestExceptionRefactoring:ERROR -Xep:ThrowsUncheckedException:ERROR -Xep:TryFailRefactoring:ERROR -Xep:TypeParameterNaming:ERROR -Xep:UngroupedOverloads:ERROR -Xep:UnnecessarySetDefault:ERROR -Xep:UnnecessaryStaticImport:ERROR -Xep:UseBinds:ERROR -Xep:WildcardImport:ERROR</arg>\n"+
	"	</compilerArgs>\n"+
	"	<annotationProcessorPaths>\n"+
	"		<path>\n"+
	"			<groupId>com.google.errorprone</groupId>\n"+
	"			<artifactId>error_prone_core</artifactId>\n"+
	"			<version>2.3.2</version>\n"+
	"		</path>\n"+
	"	</annotationProcessorPaths>\n"+
	"</configuration>\n"+
	"</plugin>\n";
	private final String PROFILE = "<profile>\n"+
	"<id>jdk8</id>\n"+
	"<activation>\n"+
	"	<jdk>1.8</jdk>\n"+
	"</activation>\n"+
	"<build>\n"+
	"	<plugins>\n"+
	"		<plugin>\n"+
	"			<groupId>org.apache.maven.plugins</groupId>\n"+
	"			<artifactId>maven-compiler-plugin</artifactId>\n"+
	"			<configuration>\n"+
	"				<fork>true</fork>\n"+
	"				<compilerArgs combine.children=\"append\">\n"+
	"					<arg>-J-Xbootclasspath/p:${settings.localRepository}/com/google/errorprone/javac/${javac.version}/javac-${javac.version}.jar</arg>\n"+
	"				</compilerArgs>\n"+
	"			</configuration>\n"+
	"		</plugin>\n"+
	"	</plugins>\n"+
	"</build>\n"+
	"</profile>\n";
	private final String PROPERTY = "<javac.version>9+181-r4173-1</javac.version>\n";
	
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
			return errors;
		} else if (log.contains("java.lang.RuntimeException")) {
			System.out.println("Maven build error");
			return null;
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

	/**
	 * Runs ErrorProne static analysis tool on a java file.
	 * TODO: run ErrorProne from source code
	 *
	 * @param file   Name of file to analyze
	 * @return       ErrorProne results
	 *
	@Override
	public String analyze(String file) {
		String cmd = RUN_CMD.replace("{file}", file);
		String output = "";
		try {
			Process p = Runtime.getRuntime().exec(cmd);	
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String line;
			while ((line = br.readLine()) != null) {
			    output += line + "\n";
			}
		} catch (IOException e) {
			return null;
		}
		return output;
	}*/
}

