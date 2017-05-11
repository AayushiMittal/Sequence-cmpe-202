import java.io.*;
import java.util.*;
import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import net.sourceforge.plantuml.SourceStringReader;


public class ParseSeqEngine {
	
    static String parseString;
    static String folderPath;
    static String imageFile;
    static HashMap<String, String> classMethodMap;
    static ArrayList<CompilationUnit> cuArray;
    static HashMap<String, ArrayList<MethodCallExpr>> mapMethodCalls;
    static boolean flag = false;
    
    public ParseSeqEngine() {
	
	}

	public static void main(String[] args) throws Exception {
		
		if (args.length == 1) {
            imageFile = "seqDiagram.png";
            folderPath = args[0];
        	} 
        	else if (args.length == 2) {
        	
        	folderPath = args[0];
            imageFile = folderPath + "\\" + args[1] + ".png";
            }
            else {
            	System.out.println("Invalid Parameters Passed \nPass in the following format\n");
    			System.out.println("seq <SourceCode Folder> <Name Of the Class> <Name of the Method> <output Image Name>");
                throw new FileNotFoundException("Invalid Folder Path/ File Not found");
            }
		
		            
            ParseSeqEngine obj = new ParseSeqEngine();
            classMethodMap = new HashMap<String, String>();
            mapMethodCalls = new HashMap<String, ArrayList<MethodCallExpr>>();
            parseString = "@startuml\n";
            obj.parseThings();
		
    }   
    

     public void parseThings() throws Exception {
        
    	
    	File folder = new File(folderPath);
    	
    	ArrayList<CompilationUnit> cUnits = new  ArrayList<>();
    	   	
    	for (File f : folder.listFiles()) {
    		
            if (f.isFile() && f.getName().endsWith(".java") && f != null) {
            	
            	if (f.getName().equals("Main.java")){
            		
            		flag = true;
            	}
            	
                FileInputStream fin = new FileInputStream(f);
                
                CompilationUnit cu;
                
                try {
                    cu = JavaParser.parse(fin);
                    cUnits.add(cu);
                    
                } 
                
                	finally 
                {
                    fin.close();
                }
            }
        }
    	
    	if(flag){
    	buildMaps(cUnits);
        parseString += "actor user \n";
        parseString += "user" + " -> " + "Main" + " : " + "main" + "\n";
        parseString += "activate " + classMethodMap.get("main") + "\n";
        parse("main");
        parseString += "@enduml";        
        OutputStream png = new FileOutputStream(imageFile);
        SourceStringReader reader = new SourceStringReader(parseString);
        String desc = reader.generateImage(png);
        
        System.out.println(parseString);
    	}
    	else {
    		System.out.println("No Main.Java Class Found\nRerun the parser with a Valid Main.java file");
    	}
    }
     
     private void buildMaps(ArrayList<CompilationUnit> cUnits) {
         for (CompilationUnit cu : cUnits) {             
        	 String className = "";             
             for (Node node : cu.getTypes()) {             	                 
                 className = ((ClassOrInterfaceDeclaration) node).getName();                 
                 for (BodyDeclaration methodBody : ((ClassOrInterfaceDeclaration) node).getMembers()) {                 	
                     if (methodBody instanceof MethodDeclaration) {
                    	 ArrayList<MethodCallExpr> methodExpr = new ArrayList<MethodCallExpr>();                         
                         for (Object bodyStatement : methodBody.getChildrenNodes()) {                         	
                             if (bodyStatement instanceof BlockStmt) {                             	
                                 for (Object es : ((Node) bodyStatement).getChildrenNodes()) {
                                     if (es instanceof ExpressionStmt) {
                                         if (((ExpressionStmt) (es)).getExpression() instanceof MethodCallExpr) {
                                         	methodExpr.add((MethodCallExpr) (((ExpressionStmt) (es)).getExpression()));
                                         }
                                     }
                                 }
                             }
                         }
                         mapMethodCalls.put(((MethodDeclaration) methodBody).getName(), methodExpr);
                         classMethodMap.put(((MethodDeclaration) methodBody).getName(), className);
                     }
                 }
             }
         }
     }

    private void parse(String callerFunc) {
    	for (MethodCallExpr theExpression : mapMethodCalls.get(callerFunc)) {  
    		String calleeMethod = theExpression.getName();
            if (classMethodMap.containsKey(calleeMethod)) {            	
                parseString += classMethodMap.get(callerFunc) + " -> " + classMethodMap.get(calleeMethod) + " : "
                        + theExpression.toStringWithoutComments() + "\n" + "activate " + classMethodMap.get(calleeMethod) + "\n";                
                parse(calleeMethod);                
                parseString += classMethodMap.get(calleeMethod) + " -->> " + classMethodMap.get(callerFunc) + "\n" + "deactivate " + classMethodMap.get(calleeMethod) + "\n";                
            }
        }
    }

    
}
