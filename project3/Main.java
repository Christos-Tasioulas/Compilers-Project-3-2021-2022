import syntaxtree.*;
import visitor.*;

import java.util.*;
import java.io.File;  
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;   
import java.io.IOException;
import java.util.Map.Entry;

import javax.lang.model.util.ElementScanner6;

public class Main {

    public static void main(String[] args) throws Exception {
        if(args.length < 1){
            System.err.println("Usage: java Main <file1> <file2> ... <fileN>");
            System.exit(1);
        }

        FileInputStream fis = null;
        for(int i = 0; i < args.length; i++)
        {
            try{
            
                fis = new FileInputStream(args[i]);

                String name = args[i].substring(0, args[i].indexOf("."));

                name = name + ".ll";

                File file = new File(name);
                if (file.createNewFile()) {
                    System.out.println("File created: " + file.getName());
                } else {
                    System.out.println("File already exists.");
                }

                MiniJavaParser parser = new MiniJavaParser(fis);
    
                Goal root = parser.Goal();
    
                System.err.println(args[i] + " parsed successfully.");
    
                LLVM_Visitor eval = new LLVM_Visitor(name);
                root.accept(eval, null);

                eval.ll_writer.close();

            }
            catch(ParseException ex){
                System.out.println(ex.getMessage());
            }
            catch(FileNotFoundException ex){
                System.err.println(ex.getMessage());
            }
            catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
            finally{
                try{
                    if(fis != null) fis.close();
                }
                catch(IOException ex){
                    System.err.println(ex.getMessage());
                }
            }

            System.out.println();
        }
        
        
    }
}


class MyVisitor extends GJDepthFirst<String, Void>{

    // List that stores all class names of a program
    List<String> classes;
    // map that stores a variable with the function it is included in (scoping)
    Map<String, String> funcVariable;
    // map that stores a class field with its class (scoping)
    Map<String, String> classObject;
    // map that stores a class function with its class (scoping)
    Map<String, String> classFunc;
    // map that stores a class with its parent class (inheritance)
    Map<String, String> inheritance;
    // map that stores a variable with its type
    Map<String, String> objectType;
    // map that stores a variable inside a function with its type
    Map<String, String> objectTypeforFunc;
    // map that stores a function with its return type
    Map<String, String> funcType;
    // map that stores a function with the type of all the arguments needed
    Map<String, String> func_argtypes;
    // temporary list that stores parameter types of current function
    List<String> param_types;
    // temporary list that stores argument types of current function call
    List<String> arg_types;
    // checks whether the variable is inside a function or not
    boolean in_func;
    // stores current scope
    String scope;
    // stores current function
    String func_scope;
    // overall offset of class fields
    int field_offset;
    // overall offset of class methods
    int func_offset;

    public MyVisitor()
    {
        classes = new ArrayList<String>();
        classObject = new HashMap<String, String>();
        classFunc = new HashMap<String, String>(); 
        inheritance = new HashMap<String, String>();
        funcVariable = new HashMap<String, String>();
        objectType = new HashMap<String, String>();
        objectTypeforFunc = new HashMap<String, String>();
        funcType = new HashMap<String, String>();
        func_argtypes = new HashMap<String, String>();
        param_types = new ArrayList<String>();
        arg_types = new ArrayList<String>();
        field_offset = 0;
        func_offset = 0;
        in_func = false;
    }


    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, Void argu) throws Exception {

       // we are inside the main function
       in_func = true;
       func_scope = "main";

       String classname = n.f1.accept(this, null);
       scope = classname;

       String args = n.f11.accept(this, null);

       String vars = "";
       if(n.f7 != null) vars = n.f14.accept(this, null);
       String statement = "";
       if(n.f8 != null) statement = n.f15.accept(this, null);

       System.out.println();

       classes = new ArrayList<String>();
       classObject = new HashMap<String, String>();
       classFunc = new HashMap<String, String>(); 
       inheritance = new HashMap<String, String>();
       funcVariable = new HashMap<String, String>();
       objectType = new HashMap<String, String>();
       objectTypeforFunc = new HashMap<String, String>();
       funcType = new HashMap<String, String>();
       func_argtypes = new HashMap<String, String>();

       return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, Void argu) throws Exception {

        /// we are not inside a function
        in_func = false;
        func_scope = "";

        String classname = n.f1.accept(this, null);
  
        // checking if the class has been declared before or not
        if(classes.contains(classname)) throw new ParseException("Semantic Error, Class: " + classname + " already exists");
        classes.add(classname);
        scope = classname;

        super.visit(n, argu);

        System.out.println();

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, Void argu) throws Exception {

        // we are not inside a function
        in_func = false;
        func_scope = "";

        String classname = n.f1.accept(this, null);

        // checking if the class has been declared before or not
        if(classes.contains(classname)) throw new ParseException("Semantic Error, Class: " + classname + " already exists");
        classes.add(classname);
        scope = classname;

        String parent_class = n.f3.accept(this, null);
        inheritance.put(classname, parent_class); // the program should remember if the class has a parent class or not

        super.visit(n, argu);

        System.out.println();

        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(VarDeclaration n, Void argu) throws Exception{
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);

        // checking if the variable has been declared before or not
        if(is_declared(name)) throw new ParseException("Semantic Error, " + name + " has already been declared");

        // if the variable is not inside a function, we will compute its offset
        if(!in_func)
        {
            classObject.put(name, scope); 
            objectType.put(name, type);
            System.out.println(scope + "." + name + " : " + field_offset);
            if(type == "int") field_offset+=4; // integer is 4 bytes
            else if(type == "boolean") field_offset++; // boolean is 1 byte 
            else field_offset+=8; // everything else is a pointer so 8 bytes  
        }
        else
        {
            if(funcVariable.get(name) != func_scope)
            {
                funcVariable.put(name, func_scope);
                objectTypeforFunc.put(name, type);
            }
        }


        return type + " " + name;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, Void argu) throws Exception {

        // we are inside a function
        in_func = true;

        String type = n.f1.accept(this, null);
        String myName = n.f2.accept(this, null);

        funcType.put(myName, type);

        // checking whether the function has been inherited from the parent class 
        boolean exists = false;

        // checking if the class that contains the function is inherited
        if(inheritance.containsKey(scope) == true)
        {
            String parent_class = inheritance.get(scope);

            // iterate each entry of hashmap
            for(Entry<String, String> entry: classFunc.entrySet()) {

                // if give value is equal to value from entry
                if(entry.getKey() == myName && entry.getValue() == parent_class) {
                    
                    // the function is inherited
                    exists = true;
                    break;
                }
            }
        }
        
        func_scope = myName;
        // we will compute the offset of the non inherited functions only
        if(!exists)
        {
            // checking if the function has been declared before or not
            if(classFunc.get(myName) == scope) throw new ParseException("Semantic Error, function " + myName + " has already been declared");
            classFunc.put(myName, scope); // inserting the function inside the scope map for later checks
            System.out.println(scope + "." + myName + " : " + func_offset);
            func_offset+=8; // functions are 8 bytes
        }

        String parameters = n.f4.accept(this, null);

        String vars = "";
        if(n.f7 != null) vars = n.f7.accept(this, null);
        String statement = "";
        if(n.f8 != null) statement = n.f8.accept(this, null);

        String returned = n.f10.accept(this, null);

        // checking if the thing the function returns the same type of thing as it is
        if(!check_return_type(myName, returned)) throw new ParseException("Semantic Error, wrong return type");
        
        // emptying and rearranging the maps for the next function
        objectTypeforFunc = new HashMap<String, String>();
        funcVariable = new HashMap<String, String>();

        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, Void argu) throws Exception {
        String ret = n.f0.accept(this, null);

        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, Void argu) throws Exception {
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);

        funcVariable.put(name, func_scope);
        objectTypeforFunc.put(name, type);

        return type + " " + name;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterTerm n, Void argu) throws Exception {
        String first_parameter = n.f0.accept(this, null);

        String rest_of_them = n.f1.accept(this, null);
        return first_parameter + rest_of_them;
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, Void argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            String parameter = node.accept(this, null);
            ret += ", " + parameter;
        }

        return ret;
    }

    public String visit(Type n, Void argu) throws Exception {
        
        return n.f0.accept(this, null);
    }

    public String visit(ArrayType n, Void argu) throws Exception{
        
        return n.f0.accept(this, null);
        
    }

    @Override
    public String visit(IntegerArrayType n, Void argu) throws Exception{
        return "int[]";
    }

    public String visit(BooleanArrayType n, Void argu) throws Exception{
        return "boolean[]";
    }

    public String visit(BooleanType n, Void argu) throws Exception{
        return "boolean";
    }

    public String visit(IntegerType n, Void argu) throws Exception{
        return "int";
    }

    public String visit(Statement n, Void argu) throws Exception{

        return n.f0.accept(this, null);
    }

    /**
     * f1 -> "{"
     * f2 -> ( Statement() )*
     * f3 -> "}"
     */
    public String visit(Block n, Void argu) throws Exception{

        return super.visit(n, argu);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public String visit(AssignmentStatement n, Void argu) throws Exception{
        String to_be_assigned = n.f0.accept(this, null);
        String assignment = n.f2.accept(this, null);
       
        // Checking if the identifiers from both ends of the assignment have the same type
        if(is_identifier(to_be_assigned) && is_identifier(assignment))
        {
            String type_a = get_expression_type(to_be_assigned);
            String type_b = get_expression_type(assignment);
            if(!(type_a.equals(type_b))) throw new ParseException("Semantic Error, wrong assignment type " + to_be_assigned + " and " + assignment);
        }
        

        // Checking if the identifier on the assigned end has been declared in current or parent scopes or not
        if(in_func)
        {
            if(is_identifier(to_be_assigned))
            {
                if(funcVariable.get(to_be_assigned) != func_scope) 
                {
                    if(classObject.get(to_be_assigned) != scope) 
                    {
                        String parent = scope;
                        while(true)
                        {
                            parent = inheritance.get(parent);
                            if(parent == null) throw new ParseException("Semantic Error, " + to_be_assigned + " undeclared");
                            else if(classObject.get(to_be_assigned) != parent) continue;
                            else break;
                        }
                        
                        
                    }
                }
                
            }
        }

        // Checking if the identifier on the assignment end has been declared in current or parent scopes or not
        if(is_identifier(assignment)) 
        {
           if(!is_declared(assignment))
           {
               throw new ParseException("Semantic Error, undeclared");
           } 
        }
        // Checking if we have allocated an array to a non array variable
        else if(is_array_allocation(assignment))
        {
            String type = objectTypeforFunc.get(to_be_assigned);
            if(type == null) type = objectType.get(to_be_assigned);
            if(!(type.equals("int[]")) && !(type.equals("boolean[]"))) throw new ParseException("Semantic Error, wrong allocation");
        }
        return to_be_assigned + "=" + assignment + ";"; 
    }


    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    @Override
    public String visit(ArrayAssignmentStatement n, Void argu) throws Exception{
        String to_be_assigned = n.f0.accept(this, null);

        // Checking if the variable to be assigned is an array variable or not
        String array_type = objectTypeforFunc.get(to_be_assigned);
        if(array_type == null) array_type = objectType.get(to_be_assigned);
        if(!(array_type.equals("int[]")) && !(array_type.equals("boolean[]"))) throw new ParseException("Semantic Error, wrong assignment type " + to_be_assigned);

        String index = n.f2.accept(this, null);
        // Checking if the index is integer or not
        if(!(get_expression_type(index).equals("int"))) throw new ParseException("Semantic Error, " + index + " is not integer"); 
        String assignment = n.f5.accept(this, null);

        // Checking if the identifier on the assigned end has been declared in current or parent scopes or not
        if(in_func)
        {
            if(is_identifier(to_be_assigned))
            {
                if(funcVariable.get(to_be_assigned) != func_scope) 
                {
                    if(classObject.get(to_be_assigned) != scope) 
                    {
                        String parent = scope;
                        while(true)
                        {
                            parent = inheritance.get(parent);
                            if(parent == null) throw new ParseException("Semantic Error, " + to_be_assigned + " undeclared");
                            else if(classObject.get(to_be_assigned) != parent) continue;
                            else break;
                        }
                        
                        
                    }
                }
                
            }
        }

         // Checking if the identifier on the assignment end has been declared in current or parent scopes or not
        if(is_identifier(assignment)) 
        {
           if(!is_declared(assignment))
           {
               throw new ParseException("Semantic Error, undeclared");
           } 
        }
        // Checking if we have allocated an array to a non array variable
        else if(is_array_allocation(assignment))
        {
            String type = objectTypeforFunc.get(to_be_assigned);
            if(type == null) type = objectType.get(to_be_assigned);
            if(!(type.equals("int[]")) && !(type.equals("boolean[]"))) throw new ParseException("Semantic Error, wrong allocation");
        }

        return to_be_assigned + "[" + index + "]=" + assignment + ";"; 
    }

    /**
     * f0 -> "If"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> "Statement()"
     */
    @Override
    public String visit(IfStatement n, Void argu) throws Exception{

        String condition = n.f2.accept(this, null);
        String if_statement = n.f4.accept(this, null);
        String else_statement = n.f6.accept(this, null);

        return "if(" + condition + ")" + if_statement + "else" + else_statement; 
    }

    /**
     * f0 -> "While"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    @Override
    public String visit(WhileStatement n, Void argu)  throws Exception{

        String condition = n.f2.accept(this, null);
        String loop = n.f4.accept(this, null);

        return "while(" + condition + ")" + loop; 
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    @Override
    public String visit(PrintStatement n, Void argu)  throws Exception{

        String printed = n.f2.accept(this, null);
        
        // checking if identifier is integer
        if(is_identifier(printed))
        {
            String type = get_expression_type(printed);
            if(!(type.equals("int"))) throw new ParseException("Semantic Error, " + printed + " is not integer");
        }
        
        return "System.out.println(" + printed + ");";
    }

    @Override
    public String visit(Expression n, Void argu) throws Exception{

        return n.f0.accept(this, null);
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public String visit(AndExpression n, Void argu) throws Exception{

        String and1 = n.f0.accept(this, null);
        String and2 = n.f2.accept(this, null);
        return and1 + "&&" + and2;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, Void argu) throws Exception{

        String small = n.f0.accept(this, null);
        String big = n.f2.accept(this, null);
        return small + "<" + big;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, Void argu) throws Exception{

        String plus1 = n.f0.accept(this, null);
        String plus2 = n.f2.accept(this, null);
        return plus1 + "+" + plus2;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, Void argu) throws Exception{

        String minus1 = n.f0.accept(this, null);
        String minus2 = n.f2.accept(this, null);
        return minus1 + "-" + minus2;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, Void argu) throws Exception{

        String times1 = n.f0.accept(this, null);
        String type1 = get_expression_type(times1);
        String times2 = n.f2.accept(this, null);
        String type2 = get_expression_type(times2);

        // Checking if all multiplication terms are integers
        if(!(type1.equals("int")) || !(type2.equals("int"))) throw new ParseException("Semantic Error, wrong multiplication");
        return times1 + "*" + times2;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, Void argu) throws Exception{

        String array = n.f0.accept(this, null);
        String array_type = objectTypeforFunc.get(array);
        if(array_type == null) array_type = objectType.get(array);
        // checking if the identifier is an array or not
        if(!(array_type.equals("int[]")) && !(array_type.equals("boolean[]"))) throw new ParseException("Semantic Error, wrong type " + array );
        String index = n.f2.accept(this, null);
        return array + "[" + index + "]";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, Void argu) throws Exception{

        String array = n.f0.accept(this, null);
        return array + ".length";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public String visit(MessageSend n, Void argu) throws Exception{

        arg_types = new ArrayList<String>();

        String classname = n.f0.accept(this, null);
        String method = n.f2.accept(this, null);
        String arguments = n.f4.accept(this, null);

        return classname + "." + method + "(" + arguments + ")";
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public String visit(ExpressionList n, Void argu) throws Exception {

        String first_argument = n.f0.accept(this, null);

        String rest_of_them = n.f1.accept(this, null);

        return first_argument + rest_of_them;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public String visit(ExpressionTail n, Void argu) throws Exception {

        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += node.accept(this, null);
        }
        return ret;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    @Override
    public String visit(ExpressionTerm n, Void argu) throws Exception {
        
        String ret = "";
        if(n.f1 != null)
        {
            String argument = n.f1.accept(this, null); 

            ret += ", " + argument;
        } 

        return ret;
    }

    @Override
    public String visit(Clause n, Void argu) throws Exception{
        
        return n.f0.accept(this, null);
    }

    @Override
    public String visit(PrimaryExpression n, Void argu) throws Exception{

        String exp = n.f0.accept(this, null);
        

        return exp;
    }

    public String visit(IntegerLiteral n, Void argu) throws Exception{

        return n.f0.toString();
    }

    public String visit(TrueLiteral n, Void argu) throws Exception{

        return "true";
    }

    public String visit(FalseLiteral n, Void argu) throws Exception{

        return "false";
    }

    @Override
    public String visit(Identifier n, Void argu) throws Exception{
        
        String myName = n.f0.toString();

        return myName;
    }

    public String visit(ThisExpression n, Void argu) throws Exception{

        return "this";
    }

    public String visit(ArrayAllocationExpression n, Void argu) throws Exception{
        
        return n.f0.accept(this, null);
    }

    /**
     * f0 -> "new"
     * f1 -> "boolean"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(BooleanArrayAllocationExpression n, Void argu) throws Exception{

        String index = n.f3.accept(this, null);

        return "new boolean [" + index + "]";
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(IntegerArrayAllocationExpression n, Void argu) throws Exception{

        String index = n.f3.accept(this, null);

        return "new int [" + index + "]";
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    @Override
    public String visit(AllocationExpression n, Void argu) throws Exception{

        String classname = n.f1.accept(this, null);

        return "new " + classname + "()";
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    @Override
    public String visit(NotExpression n, Void argu) throws Exception{

        String oppposite = n.f1.accept(this, null);

        return "!" + oppposite;
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n, Void argu) throws Exception{

        String expression = n.f1.accept(this, null);

        return "(" + expression + ")";
    }

    // checking if given expression is identifier
    private boolean is_identifier(String s) throws Exception{

        List<String> operators = new ArrayList<>(Arrays.asList("&&", "<", "+", "-", "*", ".", "(", ")", "[", "]"));
        List<String> letters = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"));
        List<String> keywords = new ArrayList<>(Arrays.asList("true", "false", "this", "new"));

        // identifiers have letters, but not operators and are not keywords
        if(operators.stream().anyMatch(s::contains)) return false;
        if(!(letters.stream().anyMatch(s::contains))) return false;
        if(keywords.stream().anyMatch(s::equals)) return false;

        return true;
    }

    // checking if given expression has brackets
    private boolean is_bracket_expression(String s) throws Exception{


        if((s.indexOf("(") == 0) && (s.indexOf(")", s.length()-1) == s.length()-1)) return true;
        else return false;
    }

    // checking if given expression is "this" 
    private boolean is_this(String s) throws Exception{
        if(s.equals("this")) return true;
        else return false;
    }

    // checking if given expression has "this" 
    private boolean is_this_expression(String s) throws Exception{
        if(s.contains("this")) return true;
        else return false;
    }

    // checking if given expression is Class.field
    private boolean is_field(String s) throws Exception{
        if(s.contains(".")) return true;
        else return false;
    }

    // checking if given expression is function call
    private boolean is_function_call(String s) throws Exception{

        List<String> letters = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"));
        List<String> keywords = new ArrayList<>(Arrays.asList("true", "false", "this", "new"));

        // function calls have letters
        if(!(letters.stream().anyMatch(s::contains))) return false;
        // function calls have parenthesis
        if(s.contains("(") && s.contains(")"))
        {
            // function names have letters and are not keywords
            String func_name = s.substring(0, s.indexOf("("));
            if(!(letters.stream().anyMatch(func_name::contains))) return false;
            if((keywords.stream().anyMatch(func_name::equals))) return false;

            return true;
        }

        return false;

    }

    // checking if given expression is array lookup
    private boolean is_array(String s) throws Exception{

        // array lookups have []
        if(s.contains("[") && s.contains("]")) return true;
        else return false;
    }

    // checking if identifier is declared
    private boolean is_declared(String s) throws Exception{

        // if it is in function, it might be declared inside it
        if(in_func)
        {
            for(Map.Entry<String, String> pair: funcVariable.entrySet())
            {
                if((pair.getKey() == s) && (pair.getValue() == func_scope)) return true;
            }
        }

        // or it might be declared inside the class 
        for(Map.Entry<String, String> pair: classObject.entrySet())
        {
            if((pair.getKey() == s) && (pair.getValue() == scope)) return true;
        }

        return false;
    }


    // checking if the expression is new int[<index>] or new boolean[<index>]
    private boolean is_array_allocation(String s) throws Exception{

        if(s.contains("new int [") && s.contains("]")) return true;
        else if(s.contains("new boolean [") && s.contains("]")) return true;
        else return false;
    }

    // checking if the expression is an allocation
    private boolean is_allocation(String s) throws Exception{

        if(s.contains("new")) return true;
        else return false;
    }

    // checking if expression is an arithmetic operation
    private boolean is_arithmetic_expression(String s) throws Exception{

        List<String> arithmetic_operators = new ArrayList<String>(Arrays.asList("+", "-", "*"));

        if((arithmetic_operators.stream().anyMatch(s::contains))) return true;
        else return false;
    }

    // returns a string that describes the type of given expression recursively. 
    private String get_expression_type(String exp) throws Exception{

        // base case if expression is identifier
        if(is_identifier(exp))
        {
            // we already have saved the type for every identifier
            String type;
            if(in_func)
            {
                type = objectTypeforFunc.get(exp);
                if(type == null) type = objectType.get(exp);
            }
            else type = objectType.get(exp);

            return type;
        }
        // base case if expression is array lookup
        else if(is_array(exp))
        {
            // if array is int[] the lookup is int
            // if array is boolean[] the lookup is boolean
            String sub = exp.substring(0, exp.indexOf("["));
            if(get_expression_type(sub).equals("boolean[]")) return "boolean";
            else return "int";
        }
        // case if expression is in brackets
        else if(is_bracket_expression(exp))
        {
            // returns the type of expression inside the brackets
            String sub = exp.substring(1, exp.length() - 1);
            return get_expression_type(sub);
        }
        // base case if the expression is "this"
        else if(is_this(exp))
        {
            // returns the scope we are in
            return scope;
        }
        // case if the expression is this.<field>
        else if(is_this_expression(exp))
        {
            // returns the type of field 
            String sub = exp.substring(5, exp.length()-1);
            return get_expression_type(sub);
        }
        // case if the expression is an allocation
        else if(is_allocation(exp))
        {
            // returns the type of allocation
            String sub = exp.substring(3, exp.length()-1);
            return get_expression_type(sub);
        }
        // case if the expression is <Class>.<field>
        else if(is_field(exp))
        {
            // returns the type of field
            String sub = exp.substring(exp.indexOf('.'), exp.length()-1);
            return get_expression_type(sub);
        }
        // base case if the expression is function call
        else if(is_function_call(exp))
        {
            // returns the type of the function
            String sub = exp.substring(0, exp.indexOf("("));
            return funcType.get(sub);
        }
        // if the expression is arithmetic then it is integer
        else if(is_arithmetic_expression(exp))
        {
            return "int";
        }
        // base case if it is a constant
        else
        {
            List<String> letters = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"));
            // integers have no letters
            if(!(letters.stream().anyMatch(exp::contains))) 
            {
                return "int";
            } 
            // true or false are boolean   
            else if(exp.equals("true") || exp.equals("false"))
            {
                return "boolean";
            }

            return "";
        }

   }

   // checking if the function given returns the correct type given the thing it returns
   private boolean check_return_type(String func_name, String returned) throws Exception 
   {
        List<String> letters = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"));

        if(is_identifier(returned) == true)
        {
            // looking up the returned identifier in the symbol table
            String s1 = objectTypeforFunc.get(returned);
            if(s1 == null)
            s1 = objectType.get(returned);
            String s2 = funcType.get(func_name);
            if(s1 != s2)
            {
                return false;
            } 
        }
        // if it has no letters it is an integer
        else if(!(letters.stream().anyMatch(returned::contains)))
        {
            String func_type = funcType.get(func_name);
            if(func_type.equals("int")) return true;
            else return false;
        }

        return true;
   }
}

// Register info about its name and its type
class Reg_info
{
    public String reg_name;
    public String reg_type;

    public Reg_info(String name, String type)
    {
        this.reg_name = name;
        this.reg_type = type;
    }

}

class LLVM_Visitor extends GJDepthFirst<String, Void> {

    // List that stores all class names of a program
    List<String> classes;
    // map that stores a variable with the function it is included in (scoping)
    Map<String, String> funcVariable;
    // map that stores a class field with its class (scoping)
    Map<String, String> classObject;
    // map that stores a class function with its class (scoping)
    Map<String, String> classFunc;
    // map that stores a class with its parent class (inheritance)
    Map<String, String> inheritance;
    // map that stores a variable with its type
    Map<String, String> objectType;
    // map that stores a variable inside a function with its type
    Map<String, String> objectTypeforFunc;
    // map that stores a function with its return type
    Map<String, String> funcType;
    // map that stores a function with the type of all the arguments needed
    Map<String, String> func_argtypes;
    // temporary list that stores parameter types of current function
    List<String> param_types;
    // temporary list that stores argument types of current function call
    List<String> arg_types;
    // checks whether the variable is inside a function or not
    boolean in_func;
    // checks whether the variable is inside a while/if condition or not
    boolean in_cond;
    // stores current scope
    String scope;
    // stores current function
    String func_scope;
    // overall offset of class fields
    int field_offset;
    // overall offset of class methods
    int func_offset;
    // counter for the temporary registers for the program
    int reg_counter;
    // temporary result store
    String result;
    // FileWriter
    FileWriter ll_writer;
    // map that stores variables with their registers
    Map<String, Reg_info> registers;
    // map that stores array registers with the index registers
    Map<String, String> array_lengths;

    public LLVM_Visitor(String filename) throws Exception
    {
        classes = new ArrayList<String>();
        classObject = new HashMap<String, String>();
        classFunc = new HashMap<String, String>(); 
        inheritance = new HashMap<String, String>();
        funcVariable = new HashMap<String, String>();
        objectType = new HashMap<String, String>();
        objectTypeforFunc = new HashMap<String, String>();
        funcType = new HashMap<String, String>();
        func_argtypes = new HashMap<String, String>();
        param_types = new ArrayList<String>();
        arg_types = new ArrayList<String>();
        field_offset = 0;
        func_offset = 0;
        in_func = false;
        reg_counter = 0;
        ll_writer = new FileWriter(filename);
        registers = new HashMap<String, Reg_info>();
        array_lengths = new HashMap<String, String>();
    }

    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public String visit(Goal n, Void argu) throws Exception {

        // to avoid warnings
        ll_writer.write("target triple = \"x86_64-pc-linux-gnu\"\n");

        // declaration of print function
        ll_writer.write("declare i32 @printf(i8*, ...)\n");

        // declaration of calloc
        ll_writer.write("declare i8* @calloc(i32, i32)\n");

        // declaration of string
        ll_writer.write("@.str = constant [4 x i8] c\"%d\\0A\\00\"\n\n");

        return n.f0.accept(this, null) + n.f1.accept(this, null) + n.f2.accept(this, null);
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, Void argu) throws Exception {

        // definition of main
        ll_writer.write("define void @main() {\n");

        String vars = n.f14.accept(this,null);

        String statements = n.f15.accept(this, null);

        // main is void so it returns void
        ll_writer.write("\tret void\n}\n");

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, Void argu) throws Exception {


        return "class" + n.f1.accept(this, null) + "{\n" + n.f3.accept(this, null) + n.f4.accept(this, null) + "\n}";
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, Void argu) throws Exception {

        return "class" + n.f1.accept(this, null) + "extends" + n.f3.accept(this, null) + "{\n" + n.f5.accept(this, null) + n.f6.accept(this, null) + "\n}";
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(VarDeclaration n, Void argu) throws Exception{
        
        String type = n.f0.accept(this, null); 
        String name = n.f1.accept(this, null);

        // inserting the register with the variable in the map with type <type>*
        Reg_info ri = new Reg_info(name, type+"*");
        registers.put(name, ri);

        // if the variable is not inside a function, we will compute its offset
        if(!in_func)
        {
            //classObject.put(name, scope); 
            objectType.put(name, type);  
        }
        else
        {
            if(funcVariable.get(name) != func_scope)
            {
                //funcVariable.put(name, func_scope);
                objectTypeforFunc.put(name, type);
            }
        }

        // we allocate a register for the variable so the register has type of <type>* 
        ll_writer.write("\t%" + name + " =  alloca " + type + "\n");
        return type + " " + name;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, Void argu) throws Exception {

        return "public" + n.f1.accept(this, null) + n.f2.accept(this, null) + "(" + n.f4.accept(this, null) + ") {\n" + n.f7.accept(this, null) + n.f8.accept(this, null) + "return" + n.f10.accept(this, null) + ";\n}";
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, Void argu) throws Exception {

        return n.f0.accept(this, null) + n.f1.accept(this, null);
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, Void argu) throws Exception {

        return n.f0.accept(this, null) + n.f1.accept(this, null);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    public String visit(FormalParameterTerm n, Void argu) throws Exception {

        return ", " + n.f1.accept(this, null);
    }

    /**
     * f0 -> ( FormalParameterTerm() )*
     */
    @Override
    public String visit(FormalParameterTail n, Void argu) throws Exception {

        return n.f0.accept(this, null);
    }

    public String visit(Type n, Void argu) throws Exception {
        
        return n.f0.accept(this, null);
    }

    public String visit(ArrayType n, Void argu) throws Exception{
        
        return n.f0.accept(this, null);
        
    }

    @Override
    // IntegerArray is int*
    public String visit(IntegerArrayType n, Void argu) throws Exception{
        return "i32*";
    }

    @Override
    // BooleanArray is boolean*
    public String visit(BooleanArrayType n, Void argu) throws Exception{
        return "i8*";
    }

    @Override
    public String visit(BooleanType n, Void argu) throws Exception{
        return "i8";
    }

    @Override
    public String visit(IntegerType n, Void argu) throws Exception{
        return "i32";
    }

    public String visit(Statement n, Void argu) throws Exception{

        return n.f0.accept(this, null);
    }

    /**
     * f0 -> "{"
     * f1 -> ( Statement() )*
     * f2 -> "}"
     */
    public String visit(Block n, Void argu) throws Exception{

        return n.f1.accept(this, null);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public String visit(AssignmentStatement n, Void argu) throws Exception{
        
        String left = n.f0.accept(this, null);
        String right = n.f2.accept(this, null);
        String type1 = get_expression_type(left);
        String type2 = get_expression_type(right);


        String reg = right;
        // Removing the .length suffix if it exists
        if(is_length(right)) reg = right.substring(0, right.indexOf("."));
        
        // Finding the similarly named register
        else if(!is_literal(right)) reg = "%" + Integer.toString(reg_counter);
        // or making it integer if it is true of false
        else reg = set_bool(reg);

        // store is assignment
        ll_writer.write("\tstore " + type2 + " " + reg + ", " + type1 + " %" + left + "\n");

        // This is for easier access of an array's length if it was found for the first time.
        // If we haven't used its length before, it won't be found using the array's name. 
        // So, we update the key of the corresponding map entry to match the current array in order to be more accessible for next uses
        String length = array_lengths.get(reg);
        if(length != null)
        {
            array_lengths.remove(reg);
            array_lengths.put(left, length);
        }

        return left + " = " + right; 
    }


    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    @Override
    public String visit(ArrayAssignmentStatement n, Void argu) throws Exception{
        
        String array = n.f0.accept(this, null);
        String index = n.f2.accept(this, null);
        String right = n.f5.accept(this, null);

        Reg_info array_ri = registers.get(array);
        String array_reg = array_ri.reg_name;
        String array_type = array_ri.reg_type;

        // Loading the array
        reg_counter++;
        String element_type = array_type.substring(0, array_type.length() - 1);
        ll_writer.write("\t%" + reg_counter + " = load " + element_type + ", " + array_type + " %" + array_reg + "\n");
        array_reg = "%" + Integer.toString(reg_counter);

        // Getting the index
        String index_reg = index;
        if(!is_literal(index))
        {
            // Loading the register that stores the index
            Reg_info index_ri = registers.get(index);
            index_reg = index_ri.reg_name;
            reg_counter++;
            ll_writer.write("\t%" + reg_counter + " = load i32, i32* " + index_reg + "\n");
        }

        reg_counter++;
        // Debugging for the types
        element_type = element_type.substring(0, element_type.length() - 1);
        array_type = array_type.substring(0, array_type.length() - 1);

        // Loading the memory space of the array within the given index
        ll_writer.write("\t%" + reg_counter + " = getelementptr inbounds " + element_type + ", " + array_type + " " + array_reg + ", i32 " + index_reg + "\n");
        String element_reg = "%" + Integer.toString(reg_counter);

        String type = get_expression_type(right);
        right = set_bool(right);
        ll_writer.write("\tstore " + type + " " + right + ", " + array_type + " " + element_reg + "\n");


        return array + "[" + index + "]=" + right + ";"; 
    }

    /**
     * f0 -> "If"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> "Statement()"
     */
    @Override
    public String visit(IfStatement n, Void argu) throws Exception{

        String if_label = "if" + Integer.toString(reg_counter);
        String else_label = "else" + Integer.toString(reg_counter);
        String end_label = "continue" + Integer.toString(reg_counter);

        // Looking at the condition
        in_cond = true;
        String condition = n.f2.accept(this, null);

        Reg_info ri = registers.get(condition);

        String cond_reg = condition;
        // If the condition has not returned a i1 register
        if(!ri.reg_type.equals("i1"))
        {
            // If the condition has returned a i8* register we load it in a i8 register
            if(ri.reg_type.equals("i8*"))
            {
                reg_counter++;
                ll_writer.write("\t%" + reg_counter + " = load i8, i8* %" + condition + "\n");
                condition = "%" + Integer.toString(reg_counter); 
            }
            // Casting it into a bit
            reg_counter++;
            cond_reg = Integer.toString(reg_counter);
            ll_writer.write("\t%" + cond_reg + " = trunc i8 " + condition + " to i1\n");
        }
        // Writing the condition
        ll_writer.write("\tbr i1 %" + cond_reg + ", label %" + if_label + ", label %" + else_label + "\n");
        in_cond = false;
        // We stopped processing the condition
        
        // These are the if statements
        ll_writer.write("\n" + if_label + ":\n");

        String if_statements = n.f4.accept(this, null);

        // End if
        ll_writer.write("\tbr label %" + end_label + "\n");

        // These are the else statements
        ll_writer.write("\n" + else_label + ":\n");

        String else_statements = n.f6.accept(this, null);

        // End else
        ll_writer.write("\tbr label %" + end_label + "\n");

        // End if/else
        ll_writer.write("\n" + end_label + ":\n");

        return "If(" + condition + ")" + if_statements + "else" + else_statements; 
    }

    /**
     * f0 -> "While"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    @Override
    public String visit(WhileStatement n, Void argu)  throws Exception{

        String while_label = "while" + Integer.toString(reg_counter);
        String loop_label = "loop" + Integer.toString(reg_counter);
        String break_label = "break" + Integer.toString(reg_counter);
        ll_writer.write("\tbr label %" + while_label + "\n");
        ll_writer.write("\n" + while_label + ":\n");

        // Looking at the condition
        in_cond = true;
        String condition = n.f2.accept(this, null);

        Reg_info ri = registers.get(condition);

        String cond_reg = condition;
        // If the condition has not returned a i1 register
        if(!ri.reg_type.equals("i1"))
        {
            // If the condition has returned a i8* register we load it in a i8 register
            if(ri.reg_type.equals("i8*"))
            {
                reg_counter++;
                ll_writer.write("\t%" + reg_counter + " = load i8, i8* %" + condition + "\n");
                condition = "%" + Integer.toString(reg_counter); 
            }
            // Casting it into a bit
            reg_counter++;
            cond_reg = Integer.toString(reg_counter);
            ll_writer.write("\t%" + cond_reg + " = trunc i8 " + condition + " to i1\n");
        }
        // Writing the condition
        ll_writer.write("\tbr i1 %" + cond_reg + ", label %" + loop_label + ", label %" + break_label + "\n");
        in_cond = false;
        // We stopped processing the condition

        // This is inside the loop
        ll_writer.write("\n" + loop_label + ":\n");

        String statements = n.f4.accept(this, null);

        // Continuing the loop
        ll_writer.write("\tbr label %" + while_label + "\n");

        // Outside the loop
        ll_writer.write("\n" + break_label + ":\n");        

        return "While(" + condition + ")" + statements; 
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    @Override
    public String visit(PrintStatement n, Void argu)  throws Exception{
        
        String expression = n.f2.accept(this, null);
        String reg = expression;

        // Loading the variable in a non pointer register
        if(!is_number(expression))
        {
            Reg_info ri = registers.get(expression);

            if(ri.reg_type.equals("i32*"))
            {
                reg = "%" + ri.reg_name;
                reg_counter++;
                String prev_reg = "%" + Integer.toString(reg_counter);
                ll_writer.write("\t" + prev_reg + " = load i32, i32* " + reg + "\n");
                reg = prev_reg;
            }
        }
       
        // Printing the integer
        reg_counter++;
        ll_writer.write("\t%" + Integer.toString(reg_counter) + " = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str, i64 0, i64 0), i32 " + reg + ")\n");
        
        return expression;
    }

    @Override
    public String visit(Expression n, Void argu) throws Exception{

        return n.f0.accept(this, null);
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public String visit(AndExpression n, Void argu) throws Exception{

        String L_and = n.f0.accept(this, null);
        String R_and = n.f2.accept(this, null);

        // case if false literal && r => returns false 
        if(L_and.equals("false")) return "false";
        else if(L_and.equals("true"))
        {
            // case if true literal && (true or false literal) => returns the right literal
            if(R_and.equals("false") || R_and.equals("true")) return R_and;
            // case if true literal && r => returns r after casted
            else
            {
                // Casting the variable and returning it afterwards
                Reg_info ri = registers.get(R_and);
                reg_counter++;
                String r_reg = "%" + Integer.toString(reg_counter);
                ll_writer.write("\t" + r_reg + " = load i8, i8* %" + ri.reg_name + "\n");

                // We are using trunc and zext for effective casting
                reg_counter++;
                ll_writer.write("\t%" + reg_counter + " = trunc i8 " + r_reg + " to i1\n");
                r_reg = "%" + Integer.toString(reg_counter);

                reg_counter++;
                ll_writer.write("\t%" + reg_counter + " = zext i1 " + r_reg + " to i8\n");
                r_reg = "%" + Integer.toString(reg_counter);

                // storing the register in the map with name its number in the counter
                Reg_info r_ri = new Reg_info(Integer.toString(reg_counter), "i8");
                registers.put(r_reg, r_ri);

                return r_reg;
            }
        }
        else
        {
            // case if l && false literal => returns false literal
            if(R_and.equals("false")) return "false";
            // case if l && true Literal => returns l after casted
            else if(R_and.equals("true")) 
            {
                // Casting the variable and returning it afterwards
                Reg_info ri = registers.get(L_and);
                reg_counter++;
                String l_reg = "%" + Integer.toString(reg_counter);
                ll_writer.write("\t" + l_reg + " = load i8, i8* %" + ri.reg_name + "\n");

                // We are using trunc and zext for effective casting
                reg_counter++;
                ll_writer.write("\t%" + reg_counter + " = trunc i8 " + l_reg + " to i1\n");
                l_reg = "%" + Integer.toString(reg_counter);

                reg_counter++;
                ll_writer.write("\t%" + reg_counter + " = zext i1 " + l_reg + " to i8\n");
                l_reg = "%" + Integer.toString(reg_counter);

                // storing the register in the map with name its number in the counter
                Reg_info l_ri = new Reg_info(Integer.toString(reg_counter), "i8");
                registers.put(l_reg, l_ri);

                return l_reg;
            }
            // default case l && r
            else
            {
                // loading the left register
                Reg_info l_ri = registers.get(L_and);
                reg_counter++;
                String l_reg = "%" + Integer.toString(reg_counter);
                ll_writer.write("\t" + l_reg + " = load i8, i8* %" + l_ri.reg_name + "\n");

                // casting it into a bit
                reg_counter++;
                ll_writer.write("\t%" + reg_counter + " = trunc i8 " + l_reg + " to i1\n");
                l_reg = "%" + Integer.toString(reg_counter);

                // if it is true we need additional steps in llvm
                reg_counter++;
                ll_writer.write("\tbr i1 " + l_reg + ", label %" + reg_counter + ", label %" + (reg_counter+3) + "\n");

                String br_label = Integer.toString(reg_counter);
                String c_label = Integer.toString(reg_counter+3); 

                // In this section we have the additional steps
                ll_writer.write("\n" + br_label + ":\n");

                // loading the right register
                Reg_info r_ri = registers.get(R_and);
                reg_counter++;
                String r_reg = "%" + Integer.toString(reg_counter);
                ll_writer.write("\t" + r_reg + " = load i8, i8* %" + r_ri.reg_name + "\n");

                // casting it into a bit
                reg_counter++;
                ll_writer.write("\t%" + reg_counter + " = trunc i8 " + r_reg + " to i1\n");
                r_reg = "%" + Integer.toString(reg_counter);

                // additional steps done
                reg_counter++;
                ll_writer.write("\tbr label %" + c_label + "\n");

                // main steps
                ll_writer.write("\n" + c_label + ":\n");

                // we compare the two variables
                reg_counter++;
                String res_reg = "%" + Integer.toString(reg_counter);
                ll_writer.write("\t" + res_reg + " = phi i1 [ false, %0 ], [" + r_reg + ", %" + br_label + "]\n");

                // recast the result into a byte
                reg_counter++;
                ll_writer.write("\t%" + reg_counter + " = zext i1 " + res_reg + " to i8\n");

                // Store the register with the result into the map and return it
                Reg_info ri = new Reg_info(Integer.toString(reg_counter), "i8");
                res_reg = "%" + Integer.toString(reg_counter);

                registers.put(res_reg, ri);

                return res_reg;

            }
        } 

    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, Void argu) throws Exception{

        String a = n.f0.accept(this, null);
        String b = n.f2.accept(this, null);

        // Checking if the left expression is a number or not
        String reg_a = "";
        if(is_number(a)) reg_a = a;
        // if it is not a number we will find its register
        else 
        {
            // Loading the variable into a non pointer register
            Reg_info ri = registers.get(a);
            if(ri.reg_type.equals("i32*"))
            {
                String a_reg = "%" + ri.reg_name;
                reg_counter++;
                reg_a = "%" + Integer.toString(reg_counter);
                ll_writer.write("\t" + reg_a + " = load i32, i32* " + a_reg +"\n");
            }
            else reg_a = "%" + ri.reg_name; 
        }
        
        // Checking if the right expression is a number or not
        String reg_b = "";
        if(is_number(b)) reg_b = b;
        // if it is not a number we will find its register
        else
        {
            // Loading the variable into a non pointer register
            Reg_info ri = registers.get(b);
            if(ri.reg_type.equals("i32*"))
            {
                String b_reg = "%" + ri.reg_name;
                reg_counter++;
                reg_b = "%" + Integer.toString(reg_counter);
                ll_writer.write("\t" + reg_b + " = load i32, i32* " + b_reg +"\n");
            }
            else reg_b = "%" + ri.reg_name; 
        }

        // Comparing the two numbers , it returns a bit
        reg_counter++;
        String comp_reg = "%" + Integer.toString(reg_counter);
        String val_c = comp_reg + " = icmp slt i32 " + reg_a + ", " + reg_b;

        // Storing the register with the bit result in the register map in case we need it for comparison
        Reg_info comp_ri = new Reg_info(Integer.toString(reg_counter), "i1");
        registers.put(comp_reg, comp_ri);

        // we don't need to cast if we are inside a condition
        if(in_cond) return comp_reg;

        // Casting the result into a byte
        reg_counter++;
        String res_reg = "%" + Integer.toString(reg_counter);
        String res = res_reg +  " = zext i1 " + comp_reg + " to i8";

        ll_writer.write("\t" + val_c + "\n\t" + res + "\n");

        // Storing the register with the result in the register map
        Reg_info ri = new Reg_info(Integer.toString(reg_counter), "i8");
        registers.put(res_reg, ri); 

        return res_reg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, Void argu) throws Exception{

        String a = n.f0.accept(this, null);
        String b = n.f2.accept(this, null);

        // Checking if the left expression is a number or not
        String reg_a = "";
        if(is_number(a)) reg_a = a;
        // if it is not a number we will find its register
        else 
        {
            Reg_info ri = registers.get(a);
            // Loading the variable into a non pointer register
            if(ri.reg_type.equals("i32*"))
            {
                String a_reg = "%" + ri.reg_name;
                reg_counter++;
                reg_a = "%" + Integer.toString(reg_counter);
                ll_writer.write("\t" + reg_a + " = load i32, i32* " + a_reg +"\n");
            }
            else reg_a = "%" + ri.reg_name; 
        }
        
        
        // Checking if the right expression is a number or not
        String reg_b = "";
        if(is_number(b)) reg_b = b;
        // if it is not a number we will find its register
        else
        {
            Reg_info ri = registers.get(b);
            // Loading the variable into a non pointer register
            if(ri.reg_type.equals("i32*"))
            {
                String b_reg = "%" + ri.reg_name;
                reg_counter++;
                reg_b = "%" + Integer.toString(reg_counter);
                ll_writer.write("\t" + reg_b + " = load i32, i32* " + b_reg +"\n");
            }
            else reg_b = "%" + ri.reg_name; 
        }
        
        // adding the two expressions
        reg_counter++;
        String res_reg = "%" + Integer.toString(reg_counter);
        ll_writer.write("\t" + res_reg +  " = add i32 " + reg_a + ", " + reg_b +"\n"); 

        // Storing the register with the result in the register map
        Reg_info ri = new Reg_info(Integer.toString(reg_counter), "i32");
        registers.put(res_reg, ri); 

        return res_reg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, Void argu) throws Exception{

        String a = n.f0.accept(this, null);
        String b = n.f2.accept(this, null);

        // Checking if the left expression is a number or not
        String reg_a = "";
        if(is_number(a)) reg_a = a;
        // if it is not a number we will find its register
        else 
        {
            Reg_info ri = registers.get(a);
            // Loading the variable into a non pointer register
            if(ri.reg_type.equals("i32*"))
            {
                String a_reg = "%" + ri.reg_name;
                reg_counter++;
                reg_a = "%" + Integer.toString(reg_counter);
                ll_writer.write("\t" + reg_a + " = load i32, i32* " + a_reg +"\n");
            }
            else reg_a = "%" + ri.reg_name; 
        }
        
        // Checking if the right expression is a number or not
        String reg_b = "";
        if(is_number(b)) reg_b = b;
        // if it is not a number we will find its register
        else
        {
            Reg_info ri = registers.get(b);
            // Loading the variable into a non pointer register
            if(ri.reg_type.equals("i32*"))
            {
                String b_reg = "%" + ri.reg_name;
                reg_counter++;
                reg_b = "%" + Integer.toString(reg_counter);
                ll_writer.write("\t" + reg_b + " = load i32, i32* " + b_reg +"\n");
            }
            else reg_b = "%" + ri.reg_name; 
        }
        
        // subtracting the right expression from the left one
        reg_counter++;
        String res_reg = "%" + Integer.toString(reg_counter);
        ll_writer.write("\t" + res_reg +  " = sub i32 " + reg_a + ", " + reg_b +"\n");

        // Storing the register with the result in the register map
        Reg_info ri = new Reg_info(Integer.toString(reg_counter), "i32");
        registers.put(res_reg, ri); 

        return res_reg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, Void argu) throws Exception{

        String a = n.f0.accept(this, null);
        String b = n.f2.accept(this, null);

        // Checking if the left expression is a number or not
        String reg_a = "";
        if(is_number(a)) reg_a = a;
        // if it is not a number we will find its register
        else 
        {
            Reg_info ri = registers.get(a);
            // Loading the variable into a non pointer register
            if(ri.reg_type.equals("i32*"))
            {
                String a_reg = "%" + ri.reg_name;
                reg_counter++;
                reg_a = "%" + Integer.toString(reg_counter);
                ll_writer.write("\t" + reg_a + " = load i32, i32* " + a_reg +"\n");
            }
            else reg_a = "%" + ri.reg_name; 
        }
        
        // Checking if the right expression is a number or not
        String reg_b = "";
        if(is_number(b)) reg_b = b;
        // if it is not a number we will find its register
        else
        {
            Reg_info ri = registers.get(b);
            // Loading the variable into a non pointer register
            if(ri.reg_type.equals("i32*"))
            {
                String b_reg = "%" + ri.reg_name;
                reg_counter++;
                reg_b = "%" + Integer.toString(reg_counter);
                ll_writer.write("\t" + reg_b + " = load i32, i32* " + b_reg +"\n");
            }
            else reg_b = "%" + ri.reg_name; 
        }
        
        // multiplying the two expression
        reg_counter++;
        String res_reg = "%" + Integer.toString(reg_counter);
        ll_writer.write("\t" + res_reg +  " = mul i32 " + reg_a + ", " + reg_b +"\n");
        
        // Storing the register with the result in the register map
        Reg_info ri = new Reg_info(Integer.toString(reg_counter), "i32");
        registers.put(res_reg, ri); 

        return res_reg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, Void argu) throws Exception{

        String array = n.f0.accept(this, null);
        String index = n.f2.accept(this, null);

        // Retrieving the array from the register map and loading it in a register one pointer above
        Reg_info array_ri = registers.get(array);
        String array_reg = array_ri.reg_name;
        String array_type = array_ri.reg_type;
        reg_counter++;
        String element_type = array_type.substring(0, array_type.length() - 1);
        ll_writer.write("\t%" + reg_counter + " = load " + element_type + ", " + array_type + " %" + array_reg + "\n");
        array_reg = "%" + Integer.toString(reg_counter);

        // finding the index
        String index_reg = index;
        if(!is_literal(index))
        {
            Reg_info index_ri = registers.get(index);
            index_reg = index_ri.reg_name;
            reg_counter++;
            ll_writer.write("\t%" + reg_counter + " = load i32, i32* " + index_reg + "\n");
        }

        // Finding the memory space in the array for the index given
        reg_counter++;
        element_type = element_type.substring(0, element_type.length() - 1);
        array_type = array_type.substring(0, array_type.length() - 1);
        ll_writer.write("\t%" + reg_counter + " = getelementptr inbounds " + element_type + ", " + array_type + " " + array_reg + ", i32 " + index_reg + "\n");
        String element_reg = "%" + Integer.toString(reg_counter);

        // Loading the element inside that memory space
        reg_counter++;
        ll_writer.write("\t%" + reg_counter + " = load " + element_type + ", " + array_type + " " + element_reg + "\n");

        // Storing the register with the element in the register map
        element_reg = "%" + Integer.toString(reg_counter);
        Reg_info element_ri = new Reg_info(Integer.toString(reg_counter), element_type);
        registers.put(element_reg, element_ri);

        return element_reg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, Void argu) throws Exception{

        String array = n.f0.accept(this, null);

        // finding the length of the array from the map with the lengths
        String length_reg = array_lengths.get(array);

        // if the length is in a register we return it with the suffix .length() for easier use in higher level parsing
        if(!is_literal(length_reg)) return length_reg + ".length";

        // if the length is a number it is easier to process
        return length_reg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public String visit(MessageSend n, Void argu) throws Exception{

        return n.f0.accept(this, null) + "." + n.f2.accept(this, null) + "(" + n.f4.accept(this, null);
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public String visit(ExpressionList n, Void argu) throws Exception {

        return n.f0.accept(this, null) + n.f1.accept(this, null);
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public String visit(ExpressionTail n, Void argu) throws Exception {

        return n.f0.accept(this, null);
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    @Override
    public String visit(ExpressionTerm n, Void argu) throws Exception {

        return ", " + n.f1.accept(this, null);
    }

    @Override
    public String visit(Clause n, Void argu) throws Exception{
        
        return n.f0.accept(this, null);
    }

    @Override
    public String visit(PrimaryExpression n, Void argu) throws Exception{

        String exp = super.visit(n, argu);
        

        return exp;
    }

    public String visit(IntegerLiteral n, Void argu) throws Exception{

        return n.f0.toString();
    }

    public String visit(TrueLiteral n, Void argu) throws Exception{

        return "true";
    }

    public String visit(FalseLiteral n, Void argu) throws Exception{

        return "false";
    }

    @Override
    public String visit(Identifier n, Void argu) throws Exception{
        
        String myName = n.f0.toString();

        return myName;
    }

    public String visit(ThisExpression n, Void argu) throws Exception{

        return "this";
    }

    public String visit(ArrayAllocationExpression n, Void argu) throws Exception{
        
        return n.f0.accept(this, null);
    }

    /**
     * f0 -> "new"
     * f1 -> "boolean"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(BooleanArrayAllocationExpression n, Void argu) throws Exception{

        String index = n.f3.accept(this, null);

        String index_reg = index;

        // We find the array size if it is not an integer literal
        if(!is_literal(index))
        {
            Reg_info ri = registers.get(index);
            index_reg = "%" + ri.reg_name;
            reg_counter++;
            ll_writer.write("\t%" + reg_counter + " = load i32, i32* " + index_reg + "\n");
            index_reg = "%" + Integer.toString(reg_counter);
        }
        
        // Allocating the boolean array
        reg_counter++;
        ll_writer.write("\t%" + reg_counter + " = call i8* @calloc(i32 " + index_reg + ", i32 1)\n");

        // We return its register
        String reg_malloc = "%" + Integer.toString(reg_counter);

        // Storing the register in the register map
        Reg_info reg_info = new Reg_info(Integer.toString(reg_counter), "i8*");
        registers.put(reg_malloc, reg_info);

        // Storing its length in the corresponding map
        array_lengths.put(reg_malloc, index_reg);

        return reg_malloc;
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(IntegerArrayAllocationExpression n, Void argu) throws Exception{

        String index = n.f3.accept(this, null);

        String index_reg = index;

        // We find the array size if it is not an integer literal
        if(!is_literal(index))
        {
            Reg_info ri = registers.get(index);
            index_reg = "%" + ri.reg_name;
            reg_counter++;
            ll_writer.write("\t%" + reg_counter + " = load i32, i32* " + index_reg + "\n");
            index_reg = "%" + Integer.toString(reg_counter);
        }
        
        // Allocating the integer array
        reg_counter++;
        ll_writer.write("\t%" + reg_counter + " = call i8* @calloc(i32 " + index_reg + ", i32 4)\n");
        String reg_malloc = "%" + Integer.toString(reg_counter);

        // We need to cast the register to i32* because calloc only returns i8*
        reg_counter++;
        ll_writer.write("\t%" + reg_counter + " = bitcast i8* " + reg_malloc + " to i32*\n");

        // We return its register
        reg_malloc = "%" + Integer.toString(reg_counter);

        // Storing the register in the register map
        Reg_info reg_info = new Reg_info(Integer.toString(reg_counter), "i32*");
        registers.put(reg_malloc, reg_info);

        // Storing its length in the corresponding map
        array_lengths.put(reg_malloc, index_reg);

        return reg_malloc;
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    @Override
    public String visit(AllocationExpression n, Void argu) throws Exception{

        String classname = n.f1.accept(this, null);

        return "new " + classname + "()";
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    @Override
    public String visit(NotExpression n, Void argu) throws Exception{

        String oppposite = n.f1.accept(this, null);

        String reg_opp = oppposite;

        // checking if the clause is a literal
        String opp_reg = "";
        // the clause is not a literal
        if(!oppposite.equals("true") && !oppposite.equals("false"))
        {
            Reg_info ri_opp = registers.get(oppposite);
            reg_opp = "%" + ri_opp.reg_name;

            // we load the variable in a non pointer register
            if(ri_opp.reg_type.equals("i8*"))
            {
                reg_counter++;
                opp_reg = "%" + Integer.toString(reg_counter);
                ll_writer.write("\t" + opp_reg + " = load i8, i8* " + reg_opp + "\n");
            }
        }
        // the clause is a literal so we turn it into 0 or 1
        else opp_reg = set_bool(reg_opp);
        

        // we cast the clause to a bit 
        reg_counter++;
        String small_reg = "%" + Integer.toString(reg_counter);
        ll_writer.write("\t" + small_reg + " = trunc i8 " + opp_reg + " to i1\n");

        // and we do clause xor 1 because it will always be equal to !clause
        reg_counter++;
        String xor_reg = "%" + Integer.toString(reg_counter);
        ll_writer.write("\t" + xor_reg + " = xor i1 " + small_reg + ", 1\n");

        // Storing the register with the bit result in the register map in case we need it for comparison
        Reg_info xor_ri = new Reg_info(Integer.toString(reg_counter), "i1");
        registers.put(xor_reg, xor_ri);

        // we don't need to cast if we are inside a condition
        if(in_cond) return xor_reg;

        // we cast the result to a byte
        reg_counter++;
        String big_reg = "%" + Integer.toString(reg_counter);
        ll_writer.write("\t" + big_reg + " = zext i1 " + xor_reg + " to i8\n");

        // Storing the register with the result in the register map
        Reg_info ri = new Reg_info(Integer.toString(reg_counter), "i8");
        registers.put(big_reg, ri);

        return big_reg;
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n, Void argu) throws Exception{

        String expression = n.f1.accept(this, null);

        return expression;
    }
    
    // returns the type of an expression
    String get_expression_type(String exp) throws Exception {

        // true or false are boolean
        if(exp.equals("true")) return "i8";
        else if(exp.equals("false")) return "i8";
        // numbers are integers
        else if(is_number(exp)) return "i32";
        // lengths are integers
        else if(is_length(exp)) return "i32";
        // base case if we have a variable
        else
        {
            Reg_info ri = registers.get(exp);
            return ri.reg_type;
        }
    }

    // checking if expression is a literal or not
    private boolean is_literal(String s) throws Exception{
        if(s.equals("true")) return true;
        if(s.equals("false")) return true;
        if(is_number(s)) return true;

        return false;
    }

    // checking if expression is a number
    private boolean is_number(String s) throws Exception{
        List<String> operators = new ArrayList<>(Arrays.asList("&&", "<", "+", "-", "*", ".", "(", ")", "[", "]", "%"));
        List<String> letters = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"));

        
        // Numbers have no letters or operators
        if(operators.stream().anyMatch(s::contains)) return false;
        if(letters.stream().anyMatch(s::contains)) return false;

        return true;
    }

    private String set_bool(String s)
    {
        // true is 1
        if(s.equals("true")) return "1";
        // false is 0
        else if(s.equals("false")) return "0";
        // everything else is itself
        else return s;
    }

    private Boolean is_length(String s)
    {
        if(s.contains(".length")) return true;
        else return false;
    }
}