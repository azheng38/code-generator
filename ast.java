import java.io.*;
import java.util.*;


// **********************************************************************
// INITIAL ast.java FOR P6 (available prior to last late day for P5) 
// - consists of ast.java provided for P5 along with the additions 
//   mentioned in the "Changes to old code" section of the write-up
//  (https://pages.cs.wisc.edu/~hasti/cs536/assignments/p6/p6.html#codeChanges)
//   EXCEPT the following part of item 6:
//   ... the typecheck method for the WriteStmtNode has been updated to 
//       fill in this field...
//   since writing the typecheck method is part of P5
// 
// The ASTnode class defines the nodes of the abstract-syntax tree that
// represents a base program.
//
// Internal nodes of the tree contain pointers to children, organized
// either in a list (for nodes that may have a variable number of 
// children) or as a fixed set of fields.
//
// The nodes for literals and identifiers contain line and character 
// number information; for string literals and identifiers, they also 
// contain a string; for integer literals, they also contain an integer 
// value.
//
// Here are all the different kinds of AST nodes and what kinds of 
// children they have.  All of these kinds of AST nodes are subclasses
// of "ASTnode".  Indentation indicates further subclassing:
//
//     Subclass              Children
//     --------              --------
//     ProgramNode           DeclListNode
//     DeclListNode          linked list of DeclNode
//     DeclNode:
//       VarDeclNode         TypeNode, IdNode, int
//       FctnDeclNode        TypeNode, IdNode, FormalsListNode, FctnBodyNode
//       FormalDeclNode      TypeNode, IdNode
//       TupleDeclNode       IdNode, DeclListNode
//
//     StmtListNode          linked list of StmtNode
//     ExpListNode           linked list of ExpNode
//     FormalsListNode       linked list of FormalDeclNode
//     FctnBodyNode          DeclListNode, StmtListNode
//
//     TypeNode:
//       LogicalNode         --- none ---
//       IntegerNode         --- none ---
//       VoidNode            --- none ---
//       TupleNode           IdNode
//
//     StmtNode:

//       PostIncStmtNode     ExpNode
//       PostDecStmtNode     ExpNode
//       IfStmtNode          ExpNode, DeclListNode, StmtListNode
//       IfElseStmtNode      ExpNode, DeclListNode, StmtListNode,
//                                    DeclListNode, StmtListNode
//       WhileStmtNode       ExpNode, DeclListNode, StmtListNode
//       ReadStmtNode        ExpNode
//       WriteStmtNode       ExpNode
//       CallStmtNode        CallExpNode
//       ReturnStmtNode      ExpNode
//
//     ExpNode:
//       TrueNode            --- none ---
//       FalseNode           --- none ---
//       IdNode              --- none ---
//       IntLitNode          --- none ---
//       StrLitNode          --- none ---
//       TupleAccessNode     ExpNode, IdNode
//       AssignExpNode       ExpNode, ExpNode
//       CallExpNode         IdNode, ExpListNode
//       UnaryExpNode        ExpNode
//         UnaryMinusNode
//         NotNode
//       BinaryExpNode       ExpNode ExpNode
//         PlusNode     
//         MinusNode
//         TimesNode
//         DivideNode
//         EqualsNode
//         NotEqualsNode
//         LessNode
//         LessEqNode
//         GreaterNode
//         GreaterEqNode
//         AndNode
//         OrNode
//
// Here are the different kinds of AST nodes again, organized according to
// whether they are leaves, internal nodes with linked lists of children, 
// or internal nodes with a fixed number of children:
//
// (1) Leaf nodes:
//        LogicalNode,  IntegerNode,  VoidNode,    IdNode,  
//        TrueNode,     FalseNode,    IntLitNode,  StrLitNode
//
// (2) Internal nodes with (possibly empty) linked lists of children:
//        DeclListNode, StmtListNode, ExpListNode, FormalsListNode
//
// (3) Internal nodes with fixed numbers of children:
//        ProgramNode,     VarDeclNode,     FctnDeclNode,  FormalDeclNode,
//        TupleDeclNode,   FctnBodyNode,    TupleNode,     AssignStmtNode,
//        PostIncStmtNode, PostDecStmtNode, IfStmtNode,    IfElseStmtNode,
//        WhileStmtNode,   ReadStmtNode,    WriteStmtNode, CallStmtNode,
//        ReturnStmtNode,  TupleAccessNode, AssignExpNode, CallExpNode,
//        UnaryExpNode,    UnaryMinusNode,  NotNode,       BinaryExpNode,   
//        PlusNode,        MinusNode,       TimesNode,     DivideNode,
//        EqualsNode,      NotEqualsNode,   LessNode,      LessEqNode,
//        GreaterNode,     GreaterEqNode,   AndNode,       OrNode
//
// **********************************************************************

// **********************************************************************
//   ASTnode class (base class for all other kinds of nodes)
// **********************************************************************

abstract class ASTnode { 
    // every subclass must provide an unparse operation
    abstract public void unparse(PrintWriter p, int indent);

    // this method can be used by the unparse methods to do indenting
    protected void doIndent(PrintWriter p, int indent) {
        for (int k=0; k<indent; k++) p.print(" ");
    }
}

// **********************************************************************
//   ProgramNode, DeclListNode, StmtListNode, ExpListNode, 
//   FormalsListNode, FctnBodyNode
// **********************************************************************

class ProgramNode extends ASTnode {
    public ProgramNode(DeclListNode L) {
        myDeclList = L;
    }

    /***
     * nameAnalysis
     * Creates an empty symbol table for the outermost scope, then processes
     * all of the globals, tuple defintions, and functions in the program.
     ***/
    public void nameAnalysis() {
        SymTable symTab = new SymTable();
        myDeclList.nameAnalysis(symTab);
        if (noMain) {
            ErrMsg.fatal(0, 0, "No main function");
        }
    }
	
    /***
     * typeCheck
     ***/
    public void typeCheck() {
        // added so P6 compiles (to be updated after last late day for P5)
    }
	
    /***
     * codeGen
     ***/
    public void codeGen() {
		myDeclList.codeGen();
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
    }

    // 1 child
    private DeclListNode myDeclList;

    public static boolean noMain = true; 
}

class DeclListNode extends ASTnode {
    public DeclListNode(List<DeclNode> S) {
        myDecls = S;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, process all of the decls in the list.
     ***/
    public void nameAnalysis(SymTable symTab) {
        nameAnalysis(symTab, symTab);
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab and a global symbol table globalTab
     * (for processing tuple names in variable decls), process all of the 
     * decls in the list.
     ***/    
    public void nameAnalysis(SymTable symTab, SymTable globalTab) {
        for (DeclNode node : myDecls) {
            if (node instanceof VarDeclNode) {
                ((VarDeclNode)node).nameAnalysis(symTab, globalTab);
            } else {
                node.nameAnalysis(symTab);
            }
        }
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator it = myDecls.iterator();
        try {
            while (it.hasNext()) {
                ((DeclNode)it.next()).unparse(p, indent);
            }
        } catch (NoSuchElementException ex) {
            System.err.println("unexpected NoSuchElementException in DeclListNode.print");
            System.exit(-1);
        }
    }

    public void codeGen() {
	for (DeclNode node : myDecls) {
	    node.codeGen();
	}
    }

    // list of children (DeclNodes)
    private List<DeclNode> myDecls;
}

class StmtListNode extends ASTnode {
    public String retLabel = "";

	public StmtListNode(List<StmtNode> S) {
        myStmts = S;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, process each statement in the list.
     ***/
    public void nameAnalysis(SymTable symTab, String retLabel) {
		this.retLabel = retLabel;
        for (StmtNode node : myStmts) {
            node.nameAnalysis(symTab, retLabel);
        }
    } 

    public void unparse(PrintWriter p, int indent) {
        Iterator<StmtNode> it = myStmts.iterator();
        while (it.hasNext()) {
            it.next().unparse(p, indent);
        } 
    }

    public void codeGen() {
		for (StmtNode node : myStmts) {
			// pass in the return label so we can jump to
			// it when it is return statment
			node.codeGen(retLabel);
		}
    }

    // list of children (StmtNodes)
    private List<StmtNode> myStmts;
}

class ExpListNode extends ASTnode {
    public ExpListNode(List<ExpNode> S) {
        myExps = S;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, process each exp in the list.
     ***/
    public void nameAnalysis(SymTable symTab) {
        for (ExpNode node : myExps) {
            node.nameAnalysis(symTab);
        }
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<ExpNode> it = myExps.iterator();
        if (it.hasNext()) {         // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) {  // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        } 
    }

    public void codeGen() {
		for (ExpNode exp : myExps) {
			exp.codeGen();
		}
    }

    // list of children (ExpNodes)
    private List<ExpNode> myExps;
}
class FormalsListNode extends ASTnode {
    public FormalsListNode(List<FormalDeclNode> S) {
        myFormals = S;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * for each formal decl in the list
     *     process the formal decl
     *     if there was no error, add type of formal decl to list
     ***/
    public List<Type> nameAnalysis(SymTable symTab) {
        List<Type> typeList = new LinkedList<Type>();
        for (FormalDeclNode node : myFormals) {
            Sym sym = node.nameAnalysis(symTab);
            if (sym != null) {
                typeList.add(sym.getType());
            }
        }
        return typeList;
    }    
    
    /***
     * Return the number of formals in this list.
     ***/
    public int length() {
        return myFormals.size();
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<FormalDeclNode> it = myFormals.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) {  // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        }
    }

    // list of children (FormalDeclNodes)
    private List<FormalDeclNode> myFormals;
}

class FctnBodyNode extends ASTnode {
    public FctnBodyNode(DeclListNode declList, StmtListNode stmtList) {
        myDeclList = declList;
        myStmtList = stmtList;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the declaration list
     * - process the statement list
     ***/
    public void nameAnalysis(SymTable symTab, String retLabel) {
        myDeclList.nameAnalysis(symTab);
        myStmtList.nameAnalysis(symTab, retLabel);
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
        myStmtList.unparse(p, indent);
    }

    public void codeGen() {
		myStmtList.codeGen();
    }

    // 2 children
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}


// **********************************************************************
// ****  DeclNode and its subclasses
// **********************************************************************

abstract class DeclNode extends ASTnode {
    /***
     * Note: a formal decl needs to return a sym
     ***/
    abstract public Sym nameAnalysis(SymTable symTab);
    abstract public void codeGen();
}

class VarDeclNode extends DeclNode {
    public VarDeclNode(TypeNode type, IdNode id, int size) {
        myType = type;
        myId = id;
        mySize = size;
    }

    /***
     * nameAnalysis (overloaded)
     * Given a symbol table symTab, do:
     * if this name is declared void, then error
     * else if the declaration is of a tuple type, 
     *     lookup type name (globally)
     *     if type name doesn't exist, then error
     * if no errors so far,
     *     if name has already been declared in this scope, then error
     *     else add name to local symbol table     
     *
     * symTab is local symbol table (say, for tuple field decls)
     * globalTab is global symbol table (for tuple type names)
     * symTab and globalTab can be the same
     ***/
    public Sym nameAnalysis(SymTable symTab) {
        return nameAnalysis(symTab, symTab);
    }
    
    public Sym nameAnalysis(SymTable symTab, SymTable globalTab) {
        boolean badDecl = false;
        String name = myId.name();
        Sym sym = null;
        IdNode tupleId = null;

        if (myType instanceof VoidNode) {  // check for void type
            ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
                         "Non-function declared void");
            badDecl = true;        
        }
        
        else if (myType instanceof TupleNode) {
            tupleId = ((TupleNode)myType).idNode();
			try {
				sym = globalTab.lookupGlobal(tupleId.name());
            
				// if the name for the tuple type is not found, 
				// or is not a tuple type
				if (sym == null || !(sym instanceof TupleDefSym)) {
					ErrMsg.fatal(tupleId.lineNum(), tupleId.charNum(), 
								"Invalid name of tuple type");
					badDecl = true;
				}
				else {
					tupleId.link(sym);
				}
			} catch (EmptySymTableException ex) {
				System.err.println("Unexpected EmptySymTableException " +
								    " in VarDeclNode.nameAnalysis");
				System.exit(-1);
			} 
        }
        
		try {
			if (symTab.lookupLocal(name) != null) {
				ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
							"Multiply-declared identifier");
				badDecl = true;            
			}
		} catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in VarDeclNode.nameAnalysis");
            System.exit(-1);
        } 
        
        if (!badDecl) {  // insert into symbol table
            try {
                if (myType instanceof TupleNode) {
                    sym = new TupleSym(tupleId);
                }
                else {
                    sym = new Sym(myType.type());
                    if (!globalTab.isGlobalScope()) {
                        int offset = globalTab.getOffset();
                        sym.setOffset(offset);
                        globalTab.setOffset(offset - 4); // vars are integer or logical
                    } else {
                            sym.setOffset(1);
                    }
                }
                symTab.addDecl(name, sym);
                myId.link(sym);
            } catch (DuplicateSymNameException ex) {
                System.err.println("Unexpected DuplicateSymNameException " +
                                   " in VarDeclNode.nameAnalysis");
                System.exit(-1);
            } catch (EmptySymTableException ex) {
                System.err.println("Unexpected EmptySymTableException " +
                                   " in VarDeclNode.nameAnalysis");
                System.exit(-1);
            }
        }
        
        return sym;
    } 

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.println(".");
    }

    public void codeGen() {
		// global variable, needs to be stored in static data area
		if (myId.sym().isGlobal()) {
			Codegen.generate(".data");
			Codegen.generate(".align 2");
			Codegen.p.println("_" + myId.name() + ": .space 4");
		}
    }

    // 3 children
    private TypeNode myType;
    private IdNode myId;
    private int mySize;  // use value NON_TUPLE if this is not a tuple type

    public static int NON_TUPLE = -1;
}

class FctnDeclNode extends DeclNode {
    public FctnDeclNode(TypeNode type,
                      IdNode id,
                      FormalsListNode formalList,
                      FctnBodyNode body) {
        myType = type;
        myId = id;
        myFormalsList = formalList;
        myBody = body;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * if this name has already been declared in this scope, then error
     * else add name to local symbol table
     * in any case, do the following:
     *     enter new scope
     *     process the formals
     *     if this function is not multiply declared,
     *         update symbol table entry with types of formals
     *     process the body of the function
     *     exit scope
     ***/
    public Sym nameAnalysis(SymTable symTab) {
        String name = myId.name();
        FctnSym sym = null;
		//String newLabel = Codegen.nextLabel();
		String retLabel = "_" + myId.name() + "_exit";
        try {
			if (symTab.lookupLocal(name) != null) {
				ErrMsg.fatal(myId.lineNum(), myId.charNum(),
							"Multiply-declared identifier");
			}
        
			else { // add function name to local symbol table

                if (name.equals("main")) {
                    ProgramNode.noMain = false; 
                }

				try {
					sym = new FctnSym(myType.type(), myFormalsList.length());
					symTab.addDecl(name, sym);
					myId.link(sym);
				} catch (DuplicateSymNameException ex) {
					System.err.println("Unexpected DuplicateSymNameException " +
									" in FctnDeclNode.nameAnalysis");
					System.exit(-1);
				} catch (EmptySymTableException ex) {
					System.err.println("Unexpected EmptySymTableException " +
									" in FctnDeclNode.nameAnalysis");
					System.exit(-1);
				}
			}
		} catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in FctnDeclNode.nameAnalysis");
            System.exit(-1);
        } 
 
        symTab.setGlobalScope(false);
        symTab.setOffset(4);  // offset of first param  
        symTab.addScope();  // add a new scope for locals and params
        
        // process the formals
        List<Type> typeList = myFormalsList.nameAnalysis(symTab);
        if (sym != null) {
            sym.addFormals(typeList);
            sym.setParamsSize(symTab.getOffset() - 4);
        }

        symTab.setOffset(-8);  // offset of first local
        int temp = symTab.getOffset();

        myBody.nameAnalysis(symTab, retLabel); // process the function body
       
        if (sym != null) {
            sym.setLocalsSize(-1*(symTab.getOffset() - temp));
        }
        symTab.setGlobalScope(true);

        try {
            symTab.removeScope();  // exit scope
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in FctnDeclNode.nameAnalysis");
            System.exit(-1);
        }
        
        return null;
    } 

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.print("{");
        myFormalsList.unparse(p, 0);
        p.println("} [");
        myBody.unparse(p, indent+4);
        p.println("]\n");
    }

    public void codeGen() {
		// Preamble
		// this is a main method
        if (myId.isMain()) {
			Codegen.generate(".text");
			Codegen.generate(".globl main");
			Codegen.genLabel("main");
		} else { // not a main method
			Codegen.generate(".text");
			Codegen.genLabel("_" + myId.name());
		}

		// Prologue
		Codegen.genPush(Codegen.RA); // push return addr
		Codegen.genPush(Codegen.FP); // push control link

		Codegen.generate("addu", Codegen.FP, Codegen.SP, "8"); // set up FP
		int local_space = myId.localsSize();
		Codegen.generate("subu", Codegen.SP, Codegen.SP, local_space); // Push space for the locals

		// body
		myBody.codeGen();

		// Epilogue:
		Codegen.genLabel("_" + myId.name() + "_exit");

		// load return address
		Codegen.generateIndexed("lw", Codegen.RA, Codegen.FP, 0);
		
		// FP holds address to which we need to restore SP
		Codegen.generate("move", Codegen.T0, Codegen.FP);
		
		// restore FP
		Codegen.generateIndexed("lw", Codegen.FP, Codegen.FP, -4);
		//int params_space = myId.paramsSize();
		//Codegen.generate("addu", Codegen.FP, Codegen.FP, params_space);
		
		//restore SP
		Codegen.generate("move", Codegen.SP, Codegen.T0);
                Codegen.generate("addi", Codegen.SP, ((FctnSym)myId.sym()).getParamsSize());
		//Codegen.generate("addi", Codegen.SP, ((FctnSym)myId.sym()).getParamsSize());

		// return
		if(!myId.isMain()) {
		    Codegen.generate("jr", Codegen.RA);
		} else {
			Codegen.generate("li", Codegen.V0, "10");
			Codegen.generate("syscall");
		}
    }

    // 4 children
    private TypeNode myType;
    private IdNode myId;
    private FormalsListNode myFormalsList;
    private FctnBodyNode myBody;
}

class FormalDeclNode extends DeclNode {
    public FormalDeclNode(TypeNode type, IdNode id) {
        myType = type;
        myId = id;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * if this formal is declared void, then error
     * else if this formal is already in the local symble table,
     *     then issue multiply declared error message and return null
     * else add a new entry to the symbol table and return that Sym
     ***/
    public Sym nameAnalysis(SymTable symTab) {
        String name = myId.name();
        boolean badDecl = false;
        Sym sym = null;
        
        if (myType instanceof VoidNode) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
                         "Non-function declared void");
            badDecl = true;        
        }
        
        try { 
			if (symTab.lookupLocal(name) != null) {
				ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
							"Multiply-declared identifier");
				badDecl = true;
			}
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in FormalDeclNode.nameAnalysis");
            System.exit(-1);
        } 
        
        if (!badDecl) {  // insert into symbol table
            try {
                int offset = symTab.getOffset();
                sym = new Sym(myType.type());
                sym.setOffset(offset);
                symTab.setOffset(offset + 4); // only integer and logical formals
                symTab.addDecl(name, sym);
                myId.link(sym);
            } catch (DuplicateSymNameException ex) {
                System.err.println("Unexpected DuplicateSymNameException " +
                                   " in FormalDeclNode.nameAnalysis");
                System.exit(-1);
            } catch (EmptySymTableException ex) {
                System.err.println("Unexpected EmptySymTableException " +
                                   " in FormalDeclNode.nameAnalysis");
                System.exit(-1);
            }
        }
        
        return sym;
    }  

    public void unparse(PrintWriter p, int indent) {
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
    }

    public void codeGen () {

    }

    // 2 children
    private TypeNode myType;
    private IdNode myId;
}

class TupleDeclNode extends DeclNode {
    public TupleDeclNode(IdNode id, DeclListNode declList) {
        myId = id;
		myDeclList = declList;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * if this name is already in the symbol table,
     *     then multiply declared error (don't add to symbol table)
     * create a new symbol table for this tuple definition
     * process the decl list
     * if no errors
     *     add a new entry to symbol table for this tuple
     ***/
    public Sym nameAnalysis(SymTable symTab) {
        String name = myId.name();
        boolean badDecl = false;
        try {
			if (symTab.lookupLocal(name) != null) {
				ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
							"Multiply-declared identifier");
				badDecl = true;            
			}
		} catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in TupleDeclNode.nameAnalysis");
            System.exit(-1);
        } 

        SymTable tupleSymTab = new SymTable();
        
        // process the fields of the tuple
        myDeclList.nameAnalysis(tupleSymTab, symTab);
        
        if (!badDecl) {
            try {   // add entry to symbol table
                TupleDefSym sym = new TupleDefSym(tupleSymTab);
                symTab.addDecl(name, sym);
                myId.link(sym);
            } catch (DuplicateSymNameException ex) {
                System.err.println("Unexpected DuplicateSymNameException " +
                                   " in TupleDeclNode.nameAnalysis");
                System.exit(-1);
            } catch (EmptySymTableException ex) {
                System.err.println("Unexpected EmptySymTableException " +
                                   " in TupleDeclNode.nameAnalysis");
                System.exit(-1);
            }
        }
        
        return null;
    } 

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("tuple ");
        myId.unparse(p, 0);
        p.println(" {");
        myDeclList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}.\n");
    }

    public void codeGen() {

    }

    // 2 children
    private IdNode myId;
	private DeclListNode myDeclList;
}

// **********************************************************************
// *****  TypeNode and its subclasses
// **********************************************************************

abstract class TypeNode extends ASTnode {
    /* all subclasses must provide a type method */
    abstract public Type type();
}

class LogicalNode extends TypeNode {
    public LogicalNode() {
    }

    /***
     * type
     ***/
    public Type type() {
        return new LogicalType();
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("logical");
    }
}

class IntegerNode extends TypeNode {
    public IntegerNode() {
    }

    /***
     * type
     ***/
    public Type type() {
        return new IntegerType();
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("integer");
    }
}

class VoidNode extends TypeNode {
    public VoidNode() {
    }
    
    /***
     * type
     ***/
    public Type type() {
        return new VoidType();
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("void");
    }
}

class TupleNode extends TypeNode {
    public TupleNode(IdNode id) {
		myId = id;
    }
 
    public IdNode idNode() {
        return myId;
    }
       
    /***
     * type
     ***/
    public Type type() {
        return new TupleType(myId);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("tuple ");
        p.print(myId.name());
    }
	
	// 1 child
    private IdNode myId;
}

// **********************************************************************
// ****  StmtNode and its subclasses
// **********************************************************************

abstract class StmtNode extends ASTnode {
    abstract public void nameAnalysis(SymTable symTab, String retLabel);
	abstract public void codeGen(String retLabel);
}

class AssignStmtNode extends StmtNode {
    public AssignStmtNode(AssignExpNode assign) {
        myAssign = assign;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab, String retLabel) {
        myAssign.nameAnalysis(symTab);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myAssign.unparse(p, -1); // no parentheses
        p.println(".");
    }

    public void codeGen(String retLabel) {
        myAssign.codeGen();
    }

    // 1 child
    private AssignExpNode myAssign;
}

class PostIncStmtNode extends StmtNode {
    public PostIncStmtNode(ExpNode exp) {
        myExp = exp;
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab, String retLabel) {
        myExp.nameAnalysis(symTab);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("++.");
    }

    public void codeGen(String retLabel) {
    	if(myExp instanceof IdNode) {  // must be due to BASE grammar
	    myExp.codeGen();
	    Codegen.genPop(Codegen.T1); // T1 = value
	    Codegen.generate("add", Codegen.T1, Codegen.T1, "1");
	    Sym sym = ((IdNode)myExp).sym();
	    if (sym.isGlobal()) {
	        Codegen.generate("sw", Codegen.T1, "_" + ((IdNode)myExp).name());
	    } else {
		Codegen.generateIndexed("sw", Codegen.T1, Codegen.FP, sym.getOffset());
	    }
	}
    }

    // 1 child
    private ExpNode myExp;
}

class PostDecStmtNode extends StmtNode {
    public PostDecStmtNode(ExpNode exp) {
        myExp = exp;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab, String retLabel) {
        myExp.nameAnalysis(symTab);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("--.");
    }

    public void codeGen(String retLabel) {
    	if(myExp instanceof IdNode) {  // must be due to BASE grammar
            myExp.codeGen();
            Codegen.genPop(Codegen.T1); // T1 = value
            Codegen.generate("sub", Codegen.T1, Codegen.T1, "1");
            Sym sym = ((IdNode)myExp).sym();
            if (sym.isGlobal()) {
                Codegen.generate("sw", Codegen.T1, "_" + ((IdNode)myExp).name());
            } else {
                Codegen.generateIndexed("sw", Codegen.T1, Codegen.FP, sym.getOffset());
            }
        }
    }

    // 1 child
    private ExpNode myExp;
}

class IfStmtNode extends StmtNode {
    public IfStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myDeclList = dlist;
        myExp = exp;
        myStmtList = slist;
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the condition
     * - enter a new scope
     * - process the decls and stmts
     * - exit the scope
     ***/
    public void nameAnalysis(SymTable symTab, String retLabel) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        myDeclList.nameAnalysis(symTab);
        myStmtList.nameAnalysis(symTab, retLabel);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IfStmtNode.nameAnalysis");
            System.exit(-1);        
        }
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if ");
        myExp.unparse(p, 0);
        p.println(" [");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("]");  
    }

    public void codeGen(String retLabel) {
		String falseLabel = Codegen.nextLabel();
	
		myExp.codeGen();
		Codegen.genPop(Codegen.T0);
		Codegen.generate("beq", Codegen.T0, Codegen.FALSE, falseLabel); // branch if false
		//myDeclList.codeGen();
		myStmtList.codeGen();
		Codegen.genLabel(falseLabel);
    }

    // 3 children
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class IfElseStmtNode extends StmtNode {
    public IfElseStmtNode(ExpNode exp, DeclListNode dlist1,
                          StmtListNode slist1, DeclListNode dlist2,
                          StmtListNode slist2) {
        myExp = exp;
        myThenDeclList = dlist1;
        myThenStmtList = slist1;
        myElseDeclList = dlist2;
        myElseStmtList = slist2;
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the condition
     * - enter a new scope
     * - process the decls and stmts of then
     * - exit the scope
     * - enter a new scope
     * - process the decls and stmts of else
     * - exit the scope
     ***/
    public void nameAnalysis(SymTable symTab, String retLabel) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        myThenDeclList.nameAnalysis(symTab);
        myThenStmtList.nameAnalysis(symTab, retLabel);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IfStmtNode.nameAnalysis");
            System.exit(-1);        
        }
        symTab.addScope();
        myElseDeclList.nameAnalysis(symTab);
        myElseStmtList.nameAnalysis(symTab, retLabel);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IfStmtNode.nameAnalysis");
            System.exit(-1);        
        }
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if ");
        myExp.unparse(p, 0);
        p.println(" [");
        myThenDeclList.unparse(p, indent+4);
        myThenStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("]");
        doIndent(p, indent);
        p.println("else [");
        myElseDeclList.unparse(p, indent+4);
        myElseStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("]"); 
    }

    public void codeGen(String retLabel) {
    	String doneLabel = Codegen.nextLabel();
		String elseLabel = Codegen.nextLabel();

		myExp.codeGen();
		Codegen.genPop(Codegen.T0); // pop return to T0
		Codegen.generate("beq", Codegen.T0, Codegen.FALSE, elseLabel); // branch to else
		//myThenDeclList.codeGen();
		myThenStmtList.codeGen();
		Codegen.generate("b", doneLabel);
		Codegen.genLabel(elseLabel);
		//myElseDeclList.codeGen();
		myElseStmtList.codeGen();
		Codegen.genLabel(doneLabel);
    }

    // 5 children
    private ExpNode myExp;
    private DeclListNode myThenDeclList;
    private StmtListNode myThenStmtList;
    private StmtListNode myElseStmtList;
    private DeclListNode myElseDeclList;
}

class WhileStmtNode extends StmtNode {
    public WhileStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myExp = exp;
        myDeclList = dlist;
        myStmtList = slist;
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the condition
     * - enter a new scope
     * - process the decls and stmts
     * - exit the scope
     ***/
    public void nameAnalysis(SymTable symTab, String retLabel) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        myDeclList.nameAnalysis(symTab);
        myStmtList.nameAnalysis(symTab, retLabel);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IfStmtNode.nameAnalysis");
            System.exit(-1);        
        }
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("while ");
        myExp.unparse(p, 0);
        p.println(" [");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("]");
    }

    public void codeGen(String retLabel) {
		String startLabel = Codegen.nextLabel();
		String doneLabel = Codegen.nextLabel();
		Codegen.genLabel(startLabel); // evaluate exp during every loop
		myExp.codeGen();
		Codegen.genPop(Codegen.T0);
		Codegen.generate("beq", Codegen.T0, Codegen.FALSE, doneLabel);
		//myDeclList.codeGen();
		myStmtList.codeGen();
		Codegen.generate("b", startLabel);
		Codegen.genLabel(doneLabel);
    }

    // 3 children
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class ReadStmtNode extends StmtNode {
    public ReadStmtNode(ExpNode e) {
        myExp = e;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab, String retLabel) {
        myExp.nameAnalysis(symTab);
    } 

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("read >> ");
        myExp.unparse(p, 0);
        p.println(".");
    }

    public void codeGen(String retLabel) {
		Codegen.generate("li", Codegen.V0, 5);
		Codegen.generate("syscall");
		if(myExp instanceof IdNode) {  // must be due to BASE grammar
		    ((IdNode)myExp).genAddr(); // push the address of variable onto stack
		    Codegen.genPop(Codegen.T0); // pop address of variable into T0
		    Codegen.generateIndexed("sw", Codegen.V0, Codegen.T0, 0); // store V0 into address stored in T0
		}
    }

    // 1 child (actually can only be an IdNode or a TupleAccessNode)
    private ExpNode myExp;
}

class WriteStmtNode extends StmtNode {
    public WriteStmtNode(ExpNode exp) {
        myExp = exp;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab, String retLabel) {
        myExp.nameAnalysis(symTab);
    }
    
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("write << ");
        myExp.unparse(p, 0);
        p.println(".");
    }

    public void codeGen(String retLabel) {
	myExp.codeGen();
		/*
		if (myType == null) {
			Codegen.genPop(Codegen.T0);
			return ;
		}*/
		
		// write int
        if (myExp instanceof StrLitNode) { // write string
            Codegen.genPop(Codegen.A0);
            Codegen.generate("li", Codegen.V0, "4");
        } else {
	    Codegen.genPop(Codegen.A0);
            Codegen.generate("li", Codegen.V0, "1");	
	}
	Codegen.generate("syscall");
    }

    // 2 children
    private ExpNode myExp;
    private Type myType;
}

class CallStmtNode extends StmtNode {
    public CallStmtNode(CallExpNode call) {
        myCall = call;
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab, String retLabel) {
        myCall.nameAnalysis(symTab);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myCall.unparse(p, indent);
        p.println(".");
    }

    public void codeGen(String retLabel) {
        myCall.codeGen(); // since we did check for non-void ret in CallExpNode, no need to pop here
    }

    // 1 child
    private CallExpNode myCall;
}

class ReturnStmtNode extends StmtNode {
    public ReturnStmtNode(ExpNode exp) {
        myExp = exp;
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child,
     * if it has one
     ***/
    public void nameAnalysis(SymTable symTab, String retLabel) {
        if (myExp != null) {
            myExp.nameAnalysis(symTab);
        }
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("return");
        if (myExp != null) {
            p.print(" ");
            myExp.unparse(p, 0);
        }
        p.println(".");
    }

    public void codeGen(String retLabel) {
		if (myExp != null) {
			myExp.codeGen();
			//if (myExp instanceof IdNode) {
				// returns a value
			//	if (!((IdNode)myExp).sym().getType().isVoidType()) {
			//		Codegen.genPop(Codegen.V0);		
			//	}	
			//} else {
			Codegen.genPop(Codegen.V0);
			//}
		} // no return value just go on to next step
		
		// can finally use the retLabel
		Codegen.generate("j", retLabel);
	
	}

    // 1 child
    private ExpNode myExp; // possibly null
}

// **********************************************************************
// ****  ExpNode and its subclasses
// **********************************************************************

abstract class ExpNode extends ASTnode {
    /***
     * Default version for nodes with no names
     ***/
    public void nameAnalysis(SymTable symTab) { }
    abstract public void codeGen();
}

class TrueNode extends ExpNode {
    public TrueNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("True");
    }

    public void codeGen() {
	Codegen.generate("li", Codegen.T0, Codegen.TRUE); // T0 = 1
        Codegen.genPush(Codegen.T0); // push(T0)
    }

    private int myLineNum;
    private int myCharNum;
}

class FalseNode extends ExpNode {
    public FalseNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("False");
    }

    public void codeGen() {
        Codegen.generate("li", Codegen.T0, Codegen.FALSE); // T0 = 0
        Codegen.genPush(Codegen.T0); // push(T0)
    }

    private int myLineNum;
    private int myCharNum;
}

class IdNode extends ExpNode {
    public IdNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    /***
     * Link the given symbol to this ID.
     ***/
    public void link(Sym sym) {
        mySym = sym;
    }
    
    /***
     * Return the name of this ID.
     ***/
    public String name() {
        return myStrVal;
    }
    
    /***
     * Return the symbol associated with this ID.
     ***/
    public Sym sym() {
        return mySym;
    }
    
    /***
     * Return the line number for this ID.
     ***/
    public int lineNum() {
        return myLineNum;
    }
    
    /***
     * Return the char number for this ID.
     ***/
    public int charNum() {
        return myCharNum;
    }    
        
    /***
     * Return the total number of bytes for all local variables.
     * HINT: This method may be useful during code generation.
     ***/
    public int localsSize() {
        if(!(mySym instanceof FctnSym)) {
            throw new IllegalStateException("cannot call local size on a non-function");
        }
        return ((FctnSym)mySym).getLocalsSize();
    }    

    /***
     * Return the total number of bytes for all parameters.
     * HINT: This method may be useful during code generation.
     ***/
    public int paramsSize() {
        if(!(mySym instanceof FctnSym)) {
            throw new IllegalStateException("cannot call local size on a non-function");
        }
        return ((FctnSym)mySym).getParamsSize();
    }   

    /***
     * Is this function main?
     * HINT: This may be useful during code generation.
     ***/
    public boolean isMain() {
        return (myStrVal.equals("main"));
    } 

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - check for use of undeclared name
     * - if ok, link to symbol table entry
     ***/
    public void nameAnalysis(SymTable symTab) {
		try {
            Sym sym = symTab.lookupGlobal(myStrVal);
            if (sym == null) {
                ErrMsg.fatal(myLineNum, myCharNum, "Undeclared identifier");
            } else {
                link(sym);
            }
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IdNode.nameAnalysis");
            System.exit(-1);
        } 
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
        if (mySym != null) {
            p.print("<" + mySym + ">");
        }
    }

    // not sure if this is correct
    public void genAddr() {
		if (mySym.isGlobal()) { // global
			Codegen.generate("la", Codegen.T0, "_" + myStrVal);
		} else { // local
			Codegen.generateIndexed("la", Codegen.T0, Codegen.FP, mySym.getOffset()); 
		}
		Codegen.genPush(Codegen.T0);
    }

    // not sure if this is correct
    public void codeGen() {

	        if (mySym.isGlobal()) { // global variable
			Codegen.generate("lw", Codegen.T0, "_" + myStrVal);
		} else { // local variable
			Codegen.generateIndexed("lw", Codegen.T0, Codegen.FP, mySym.getOffset());
		}
		Codegen.genPush(Codegen.T0);
    }

    public void genJumpAndLink() {
	if (myStrVal.equals("main")) Codegen.generate("jal", "main");
	else Codegen.generate("jal", "_" + myStrVal); // jal _fctnName
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
    private Sym mySym;
}

class IntLitNode extends ExpNode {
    public IntLitNode(int lineNum, int charNum, int intVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myIntVal = intVal;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myIntVal);
    }

    public void codeGen() {
		Codegen.generate("li", Codegen.T0, myIntVal);
		Codegen.genPush(Codegen.T0);
    }

    private int myLineNum;
    private int myCharNum;
    private int myIntVal;
}

class StrLitNode extends ExpNode {

    public StrLitNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
    }

    public void codeGen() {
		// hashtable to check if string literal is already stored in
		// static data area
		
		// hash table does not contain string lit; this string is not
		// stored in static data area yet
		if (!stringStored.containsKey(myStrVal)) {
			label = Codegen.nextLabel(); // generate a new label
			Codegen.p.println("\t.data");
			Codegen.p.println(label + ": .asciiz " + myStrVal);
			Codegen.generate(".text");
			Codegen.generate("la", Codegen.T0, label);
			Codegen.genPush(Codegen.T0);
	
    	    // add it to the hashtable
	        stringStored.put(myStrVal, label);
		
	    } else { // string has been stored in static data area
	        label = stringStored.get(myStrVal);	
		
    	    Codegen.generate(".text");
            Codegen.generate("la", Codegen.T0, label);
	        Codegen.genPush(Codegen.T0);
	    }
    }

	public static HashMap<String, String> stringStored = new HashMap<String, String>();
    public String label; // this is the label of this string literal
    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
}

class TupleAccessNode extends ExpNode {
    public TupleAccessNode(ExpNode loc, IdNode id) {
        myLoc = loc;	
        myId = id;
    }

    /***
     * Return the symbol associated with this colon-access node.
     ***/
    public Sym sym() {
        return mySym;
    }    
    
    /***
     * Return the line number for this colon-access node. 
     * The line number is the one corresponding to the RHS of the colon-access.
     ***/
    public int lineNum() {
        return myId.lineNum();
    }
    
    /***
     * Return the char number for this colon-access node.
     * The char number is the one corresponding to the RHS of the colon-access.
     ***/
    public int charNum() {
        return myId.charNum();
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the LHS of the colon-access
     * - process the RHS of the colon-access
     * - if the RHS is of a tuple type, set the sym for this node so that
     *   a colon-access "higher up" in the AST can get access to the symbol
     *   table for the appropriate tuple definition
     ***/
    public void nameAnalysis(SymTable symTab) {
        badAccess = false;
        SymTable tupleSymTab = null; // to lookup RHS of colon-access
        Sym sym = null;
        
        myLoc.nameAnalysis(symTab);  // do name analysis on LHS
        
        // if myLoc is really an ID, then sym will be a link to the ID's symbol
        if (myLoc instanceof IdNode) {
            IdNode id = (IdNode)myLoc;
            sym = id.sym();
            
            // check ID has been declared to be of a tuple type
            
            if (sym == null) { // ID was undeclared
                badAccess = true;
            }
            else if (sym instanceof TupleSym) { 
                // get symbol table for tuple type
                Sym tempSym = ((TupleSym)sym).getTupleType().sym();
                tupleSymTab = ((TupleDefSym)tempSym).getSymTable();
            } 
            else {  // LHS is not a tuple type
                ErrMsg.fatal(id.lineNum(), id.charNum(), 
                             "Colon-access of non-tuple type");
                badAccess = true;
            }
        }
        
        // if myLoc is really a colon-access (i.e., myLoc was of the form
        // LHSloc.RHSid), then sym will either be
        // null - indicating RHSid is not of a tuple type, or
        // a link to the Sym for the tuple type RHSid was declared to be
        else if (myLoc instanceof TupleAccessNode) {
            TupleAccessNode loc = (TupleAccessNode)myLoc;
            
            if (loc.badAccess) {  // if errors in processing myLoc
                badAccess = true; // don't continue proccessing this colon-access
            }
            else { //  no errors in processing myLoc
                sym = loc.sym();

                if (sym == null) {  // no tuple in which to look up RHS
                    ErrMsg.fatal(loc.lineNum(), loc.charNum(), 
                                 "Colon-access of non-tuple type");
                    badAccess = true;
                }
                else {  // get the tuple's symbol table in which to lookup RHS
                    if (sym instanceof TupleDefSym) {
                        tupleSymTab = ((TupleDefSym)sym).getSymTable();
                    }
                    else {
                        System.err.println("Unexpected Sym type in TupleAccessNode");
                        System.exit(-1);
                    }
                }
            }

        }
        
        else { // don't know what kind of thing myLoc is
            System.err.println("Unexpected node type in LHS of colon-access");
            System.exit(-1);
        }
        
        // do name analysis on RHS of colon-access in the tuple's symbol table
        if (!badAccess) {
			try {
				sym = tupleSymTab.lookupGlobal(myId.name()); // lookup
				if (sym == null) { // not found - RHS is not a valid field name
					ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
								"Invalid tuple field name");
					badAccess = true;
				}
            
				else {
					myId.link(sym);  // link the symbol
					// if RHS is itself as tuple type, link the symbol for its tuple 
					// type to this colon-access node (to allow chained colon-access)
					if (sym instanceof TupleSym) {
						mySym = ((TupleSym)sym).getTupleType().sym();
					}
				}
			} catch (EmptySymTableException ex) {
				System.err.println("Unexpected EmptySymTableException " +
								" in TupleAccessNode.nameAnalysis");
				System.exit(-1);
			} 
        }
    }    

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myLoc.unparse(p, 0);
        p.print("):");
        myId.unparse(p, 0);
    }

    public void codeGen() {

    }

    // 4 children
    private ExpNode myLoc;	
    private IdNode myId;
    private Sym mySym;          // link to Sym for tuple type
    private boolean badAccess;  // to prevent multiple, cascading errors
}

class AssignExpNode extends ExpNode {
    public AssignExpNode(ExpNode lhs, ExpNode exp) {
        myLhs = lhs;
        myExp = exp;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's 
     * two children
     ***/
    public void nameAnalysis(SymTable symTab) {
        myLhs.nameAnalysis(symTab);
        myExp.nameAnalysis(symTab);
    }

    public void unparse(PrintWriter p, int indent) {
        if (indent != -1)  p.print("(");
        myLhs.unparse(p, 0);
        p.print(" = ");
        myExp.unparse(p, 0);
        if (indent != -1)  p.print(")");    
    }

	public void codeGen() { 
		// LHS
		if (myLhs instanceof IdNode) {
			// put lhs address into T0 and push
			((IdNode)myLhs).genAddr();
		} else {
			myLhs.codeGen();
		}
	
		// RHS
		myExp.codeGen(); // evaluate and push; leave on stack

		Codegen.genPop(Codegen.T1); // RHS value popped into t1
		Codegen.genPop(Codegen.T0); // LHS address popped into t0
		
		Codegen.generateIndexed("sw", Codegen.T1, Codegen.T0, 0); // store value in $t1 at address held in $t0
		
		//Codegen.genPush(Codegen.T1); // push $t1 back onto stack
	}

    // 2 children
    private ExpNode myLhs;
    private ExpNode myExp;
}

class CallExpNode extends ExpNode {
    public CallExpNode(IdNode name, ExpListNode elist) {
        myId = name;
        myExpList = elist;
    }

    public CallExpNode(IdNode name) {
        myId = name;
        myExpList = new ExpListNode(new LinkedList<ExpNode>());
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's 
     * two children
     ***/
    public void nameAnalysis(SymTable symTab) {
        myId.nameAnalysis(symTab);
        myExpList.nameAnalysis(symTab);
    } 

    // **** unparse ****
    public void unparse(PrintWriter p, int indent) {
        myId.unparse(p, 0);
        p.print("(");
        if (myExpList != null) {
            myExpList.unparse(p, 0);
        }
        p.print(")");   
    }

    public void codeGen() {
        if (myExpList != null) myExpList.codeGen(); // step 1
		myId.genJumpAndLink(); // step 2
		if(!(myId.sym().getType().isVoidType())) // step 3
			Codegen.genPush(Codegen.V0); 
    }

    // 2 children
    private IdNode myId;
    private ExpListNode myExpList;  // possibly null
}

abstract class UnaryExpNode extends ExpNode {
    public UnaryExpNode(ExpNode exp) {
        myExp = exp;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }
    
    // 1 child
    protected ExpNode myExp;
}

abstract class BinaryExpNode extends ExpNode {
    public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
        myExp1 = exp1;
        myExp2 = exp2;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's 
     * two children
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp1.nameAnalysis(symTab);
        myExp2.nameAnalysis(symTab);
    }
    
    // 2 children
    protected ExpNode myExp1;
    protected ExpNode myExp2;
}

// **********************************************************************
// ****  Subclasses of UnaryExpNode
// **********************************************************************

class NotNode extends UnaryExpNode {
    public NotNode(ExpNode exp) {
        super(exp);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(~");
        myExp.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        String trueLabel = Codegen.nextLabel();
	String falseLabel = Codegen.nextLabel();
	myExp.codeGen(); 
	Codegen.genPop(Codegen.T0);
	Codegen.generate("beq", Codegen.T0, Codegen.TRUE, trueLabel); // b if T0 == 1
	Codegen.generate("add", Codegen.T0, Codegen.T0, 1); // If T0 == 0 then T0 += 1
	Codegen.generate("b", falseLabel); // to go end
	Codegen.genLabel(trueLabel);
	Codegen.generate("sub", Codegen.T0, Codegen.T0, 1); // if T0 == 1 then T0 -= 1
	Codegen.genLabel(falseLabel);
	Codegen.genPush(Codegen.T0);
    }
}

class UnaryMinusNode extends UnaryExpNode {
    public UnaryMinusNode(ExpNode exp) {
        super(exp);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(-");
        myExp.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        myExp.codeGen();
	Codegen.genPop(Codegen.T0);
	Codegen.generate("neg", Codegen.T0, Codegen.T0); // T0 = -T0
	Codegen.genPush(Codegen.T0);
    }
}

// **********************************************************************
// ****  Subclasses of BinaryExpNode
// **********************************************************************

class PlusNode extends BinaryExpNode {
    public PlusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" + ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        myExp1.codeGen(); // push LHS
	    myExp2.codeGen(); // push RHS
 	    Codegen.genPop(Codegen.T1); // pop RHS
	    Codegen.genPop(Codegen.T0); // pop LHS
	    Codegen.generate("add", Codegen.T0, Codegen.T0, Codegen.T1);
	    Codegen.genPush(Codegen.T0);
    }
}

class MinusNode extends BinaryExpNode {
    public MinusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" - ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        myExp2.codeGen(); // push RHS
        myExp1.codeGen(); // push LHS
        Codegen.genPop(Codegen.T0); // pop LHS
        Codegen.genPop(Codegen.T1); // pop RHS
        Codegen.generate("sub", Codegen.T0, Codegen.T0, Codegen.T1);
        Codegen.genPush(Codegen.T0);
    }
}

class TimesNode extends BinaryExpNode {
    public TimesNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" * ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        myExp2.codeGen(); // push RHS
        myExp1.codeGen(); // push LHS
        Codegen.genPop(Codegen.T0); // pop LHS
        Codegen.genPop(Codegen.T1); // pop RHS
        Codegen.generate("mult", Codegen.T0, Codegen.T1);
	Codegen.generate("mflo", Codegen.T0);
        Codegen.genPush(Codegen.T0);
    }
}

class DivideNode extends BinaryExpNode {
    public DivideNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" / ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        myExp2.codeGen(); // push RHS
        myExp1.codeGen(); // push LHS
        Codegen.genPop(Codegen.T0); // pop LHS
        Codegen.genPop(Codegen.T1); // pop RHS
        Codegen.generate("div", Codegen.T0, Codegen.T1);
        Codegen.generate("mflo", Codegen.T0);
        Codegen.genPush(Codegen.T0);
    }
}

class EqualsNode extends BinaryExpNode {
    public EqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" == ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        myExp2.codeGen(); // RHS
        myExp1.codeGen(); // LHS
    	Codegen.genPop(Codegen.T0); // LHS
	Codegen.genPop(Codegen.T1); // RHS
	Codegen.generate("seq", Codegen.T0, Codegen.T0, Codegen.T1); // T0 = 1 if T0 == T1
	Codegen.genPush(Codegen.T0);
    }
}

class NotEqualsNode extends BinaryExpNode {
    public NotEqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" ~= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        myExp2.codeGen(); // RHS
        myExp1.codeGen(); // LHS
        Codegen.genPop(Codegen.T0); // LHS
        Codegen.genPop(Codegen.T1); // RHS
        Codegen.generate("sne", Codegen.T0, Codegen.T0, Codegen.T1); // T0 = 1 if T0 != T1
        Codegen.genPush(Codegen.T0);
    }
}

class GreaterNode extends BinaryExpNode {
    public GreaterNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" > ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        myExp2.codeGen(); // RHS
        myExp1.codeGen(); // LHS
        Codegen.genPop(Codegen.T0); // LHS
        Codegen.genPop(Codegen.T1); // RHS
        Codegen.generate("sgt", Codegen.T0, Codegen.T0, Codegen.T1); // T0 = 1 if T0 > T1
        Codegen.genPush(Codegen.T0);
    }
}

class GreaterEqNode extends BinaryExpNode {
    public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" >= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        myExp2.codeGen(); // RHS
        myExp1.codeGen(); // LHS
        Codegen.genPop(Codegen.T0); // LHS
        Codegen.genPop(Codegen.T1); // RHS
        Codegen.generate("sge", Codegen.T0, Codegen.T0, Codegen.T1); // T0 = 1 if T0 >= T1
        Codegen.genPush(Codegen.T0);
    }
}

class LessNode extends BinaryExpNode {
    public LessNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" < ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        myExp2.codeGen(); // RHS
        myExp1.codeGen(); // LHS
        Codegen.genPop(Codegen.T0); // LHS
        Codegen.genPop(Codegen.T1); // RHS
        Codegen.generate("slt", Codegen.T0, Codegen.T0, Codegen.T1); // T0 = 1 if T0 < T1
        Codegen.genPush(Codegen.T0);
    }
}

class LessEqNode extends BinaryExpNode {
    public LessEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" <= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        myExp2.codeGen(); // RHS
        myExp1.codeGen(); // LHS
        Codegen.genPop(Codegen.T0); // LHS
        Codegen.genPop(Codegen.T1); // RHS
        Codegen.generate("sle", Codegen.T0, Codegen.T0, Codegen.T1); // T0 = 1 if T0 <= T1
        Codegen.genPush(Codegen.T0);
    }
}

class AndNode extends BinaryExpNode {
    public AndNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" & ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        String falseLabel = Codegen.nextLabel();

	// first LHS
	myExp1.codeGen();
	Codegen.genPop(Codegen.T0);
	Codegen.generate("beq", Codegen.T0, Codegen.FALSE, falseLabel); // branch if LHS == FALSE
	
	// RHS
	myExp2.codeGen();
        Codegen.genPop(Codegen.T0);
        Codegen.generate("beq", Codegen.T0, Codegen.FALSE, falseLabel); // branch if RHS == FALSE
	
	Codegen.genLabel(falseLabel);
	Codegen.genPush(Codegen.T0); // push result
    }
}

class OrNode extends BinaryExpNode {
    public OrNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" | ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        String trueLabel = Codegen.nextLabel();

        // first LHS
        myExp1.codeGen();
        Codegen.genPop(Codegen.T0);
        Codegen.generate("beq", Codegen.T0, Codegen.TRUE, trueLabel); // branch if LHS == TRUE

        // RHS
        myExp2.codeGen();
        Codegen.genPop(Codegen.T0);
        Codegen.generate("beq", Codegen.T0, Codegen.TRUE, trueLabel); // branch if RHS == TRUE

        Codegen.genLabel(trueLabel);
        Codegen.genPush(Codegen.T0); // push result
    }
}
