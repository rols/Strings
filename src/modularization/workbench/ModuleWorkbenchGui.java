package modularization.workbench;

import helpers.PrettyLogRecord;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import modularization.Module;
import modularization.ModuleImpl;
import parallelization.CallbackProcess;
import parallelization.CallbackReceiverImpl;

public class ModuleWorkbenchGui extends CallbackReceiverImpl implements TreeModelListener, ActionListener {
	
	protected static final String ACTION_STARTNEWMODULETREE = "ACTION_STARTNEWMODULETREE";
	protected static final String ACTION_ADDMODULETOTREE = "ACTION_ADDMODULETOTREE";
	protected static final String ACTION_RUNMODULES = "ACTION_RUNMODULES";
	protected static final String ACTION_EDITMODULE = "ACTION_EDITMODULE";

	// Icons
	public static final ImageIcon ICON_NEW_TREE = new ImageIcon("resources/icons/reload.png");
	public static final ImageIcon ICON_ADD_MODULE = new ImageIcon("resources/icons/add.png");
	public static final ImageIcon ICON_RUN = new ImageIcon("resources/icons/forward.png");
	public static final ImageIcon ICON_EDIT_MODULE = new ImageIcon("resources/icons/configure.png");
	
	private JFrame frame;
	private ModuleWorkbenchController controller;
	private JTree moduleTree;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ModuleWorkbenchController controller = new ModuleWorkbenchController();
					ModuleWorkbenchGui window = new ModuleWorkbenchGui(controller);
					controller.getModuleTree().addCallbackReceiver(window);
					window.frame.setVisible(true);
					
					Logger.getGlobal().log(Level.INFO, "Workbench GUI started.");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ModuleWorkbenchGui(ModuleWorkbenchController controller) {
		this.controller = controller;
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Module Workbench");
		
		JSplitPane topSplitPane = new JSplitPane();
		topSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		
		JSplitPane splitPane = new JSplitPane();
		
		JPanel availableModulesPanel = new JPanel();
		splitPane.setLeftComponent(availableModulesPanel);
		availableModulesPanel.setLayout(new BorderLayout(0, 0));
		
		// Initialize available modules list
		JList<Module> list = new JList<Module>(this.controller.getAvailableModules().toArray(new Module[this.controller.getAvailableModules().size()]));
		list.addListSelectionListener(this.controller);
		availableModulesPanel.add(list);
		
		JPanel moduleTreePanel = new JPanel();
		splitPane.setRightComponent(moduleTreePanel);
		moduleTreePanel.setLayout(new BorderLayout(0, 0));
		
		// Instantiate new JTree with a custom TreeCellRenderer
		this.moduleTree = new JTree(this.controller.getModuleTree().getModuleTree());
		TreeCellRenderer moduleTreeCellRenderer = new ModuleTreeCellRenderer();
		this.moduleTree.setCellRenderer(moduleTreeCellRenderer);
		this.moduleTree.addTreeSelectionListener(this.controller);
		this.moduleTree.getModel().addTreeModelListener(this);
		moduleTreePanel.add(this.moduleTree);
		
		JToolBar toolBar = new JToolBar();
		moduleTreePanel.add(toolBar, BorderLayout.SOUTH);
		
		
		// Define toolbar buttons
		
		JButton startNewModuleTreeButton = new JButton();
		startNewModuleTreeButton.setActionCommand(ACTION_STARTNEWMODULETREE);
		startNewModuleTreeButton.setIcon(ICON_NEW_TREE);
		startNewModuleTreeButton.addActionListener(this);
		startNewModuleTreeButton.setText("new tree");
		startNewModuleTreeButton.setToolTipText("Clears the current module tree and creates a new one based on the selected module type.");
		
		JButton addModuleButton = new JButton();
		addModuleButton.setActionCommand(ACTION_ADDMODULETOTREE);
		addModuleButton.setIcon(ICON_ADD_MODULE);
		addModuleButton.addActionListener(this);
		addModuleButton.setText("add module");
		addModuleButton.setToolTipText("Adds a module as a child to the one currently selected in the tree.");
		
		JButton runModulesButton = new JButton();
		runModulesButton.setActionCommand(ACTION_RUNMODULES);
		runModulesButton.setIcon(ICON_RUN);
		runModulesButton.addActionListener(this);
		runModulesButton.setText("run");
		runModulesButton.setToolTipText("Starts the processing of the module tree.");
		
		JButton editModuleButton = new JButton();
		editModuleButton.setActionCommand(ACTION_EDITMODULE);
		editModuleButton.setIcon(ICON_EDIT_MODULE);
		editModuleButton.addActionListener(this);
		editModuleButton.setText("edit");
		editModuleButton.setToolTipText("Lets you edit or review the properties of the module that is currently chosen in the tree.");
		
		toolBar.add(startNewModuleTreeButton);
		toolBar.add(addModuleButton);
		toolBar.add(runModulesButton);
		toolBar.add(editModuleButton);
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		
		DefaultListModel<PrettyLogRecord> messageListModel = new DefaultListModel<PrettyLogRecord>();
		JList<PrettyLogRecord> messageList = new JList<PrettyLogRecord>(messageListModel);
		this.controller.getListLoggingHandler().setListModel(messageListModel);
		scrollPane.setViewportView(messageList);
		panel.add(scrollPane, BorderLayout.CENTER);
		
		topSplitPane.setLeftComponent(splitPane);
		topSplitPane.setRightComponent(panel);
		frame.getContentPane().add(topSplitPane, BorderLayout.CENTER);
		
	}

	/* (non-Javadoc)
	 * @see parallelization.CallbackReceiverImpl#receiveCallback(java.lang.Object, parallelization.CallbackProcess, boolean)
	 */
	@Override
	public void receiveCallback(Object processingResult,
			CallbackProcess process, boolean repeat) {
		// Inserting a hook here -- if the process sending the callback is a module, we update the GUI tree display
		if (ModuleImpl.class.isAssignableFrom(process.getClass())){
			this.moduleTree.revalidate();
		}
		super.receiveCallback(processingResult, process, repeat);
	}

	/* (non-Javadoc)
	 * @see parallelization.CallbackReceiverImpl#receiveException(parallelization.CallbackProcess, java.lang.Exception)
	 */
	@Override
	public void receiveException(CallbackProcess process, Exception exception) {
		// Inserting a hook here -- if the process sending the callback is a module, we update the GUI tree display
		if (ModuleImpl.class.isAssignableFrom(process.getClass())){
			this.moduleTree.revalidate();
		}
		super.receiveException(process, exception);
	}

	@Override
	public void treeNodesChanged(TreeModelEvent e) {
		System.out.println("Changed node at "+e.getTreePath().toString());
	}

	@Override
	public void treeNodesInserted(TreeModelEvent e) {
		System.out.println("Inserted new node at "+e.getTreePath().toString());
		Object[] children = e.getChildren();
		if (children.length>0 && DefaultMutableTreeNode.class.isAssignableFrom(children[0].getClass())){
			TreePath newNodePath = new TreePath(((DefaultMutableTreeNode)children[0]).getPath());
			System.out.println(newNodePath.toString());
			this.moduleTree.setSelectionPath(newNodePath);
		}
	}

	@Override
	public void treeNodesRemoved(TreeModelEvent e) {
	}

	@Override
	public void treeStructureChanged(TreeModelEvent e) {
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(ACTION_STARTNEWMODULETREE)){
			
			// New module to create
			Module rootModule;
			try {
				if (this.controller.getSelectedModule() == null)
					throw new Exception("Please do select a module from the lefthand list first.");
				rootModule = this.controller.getNewInstanceOfSelectedModule(null);
				this.controller.startNewModuleTree(rootModule);
				
			} catch (Exception e1) {
				Logger.getLogger(this.getClass().getCanonicalName()).log(Level.WARNING, "Could not create a new module tree.", e1);
			}
			
		} else if (e.getActionCommand().equals(ACTION_ADDMODULETOTREE)){
			
			try {
				// Determine module that is currently selected within the module tree
				Module parentModule = (Module) this.controller.getSelectedTreeNode().getUserObject();
				Module newModule = this.controller.getNewInstanceOfSelectedModule(this.controller.getModuleTree());
						
				// Add new module to selected tree node
				this.controller.getModuleTree().addModule(newModule, parentModule);
				
			} catch (Exception e1) {
				Logger.getLogger(this.getClass().getCanonicalName()).log(Level.WARNING, "The selected module could not be added to the tree.", e1);
			}
			
		} else if (e.getActionCommand().equals(ACTION_EDITMODULE)){
			
			try {
				// Determine module that is currently selected within the module tree
				final Module selectedModule = (Module) this.controller.getSelectedTreeNode().getUserObject();
						
				// Create new editor dialogue in separate thread
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						try {
							ModulePropertyEditor modulePropertyEditor = new ModulePropertyEditor(selectedModule);
							modulePropertyEditor.setVisible(true);
							
							Logger.getGlobal().log(Level.INFO, "Opened editor for "+selectedModule.getName()+".");
						} catch (Exception e) {
							Logger.getLogger("").log(Level.WARNING, "Could not open editor for module "+selectedModule.getName()+".", e);
						}
					}
				});
				
			} catch (Exception e1) {
				Logger.getLogger(this.getClass().getCanonicalName()).log(Level.WARNING, "Could not display editor dialogue.", e1);
			}
			
		} else if (e.getActionCommand().equals(ACTION_RUNMODULES)){
			
			try {
				this.controller.getModuleTree().runModules();
				
			} catch (Exception e1) {
				Logger.getLogger(this.getClass().getCanonicalName()).log(Level.WARNING, "Sorry, but I wasn't able to run the modules.", e1);
			}
			
		} else {
			Logger.getLogger(this.getClass().getCanonicalName()).log(Level.WARNING, "Sorry, but this command is unknown to me: "+e.getActionCommand());
		}
	}
}
