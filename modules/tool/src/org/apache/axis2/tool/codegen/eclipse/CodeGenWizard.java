package org.apache.axis2.tool.codegen.eclipse;

import org.apache.axis2.tool.codegen.Java2WSDLGenerator;
import org.apache.axis2.tool.codegen.WSDL2JavaGenerator;
import org.apache.axis2.tool.codegen.eclipse.plugin.CodegenWizardPlugin;
import org.apache.axis2.tool.codegen.eclipse.ui.AbstractWizardPage;
import org.apache.axis2.tool.codegen.eclipse.ui.JavaSourceSelectionPage;
import org.apache.axis2.tool.codegen.eclipse.ui.JavaWSDLOptionsPage;
import org.apache.axis2.tool.codegen.eclipse.ui.JavaWSDLOutputLocationPage;
import org.apache.axis2.tool.codegen.eclipse.ui.OptionsPage;
import org.apache.axis2.tool.codegen.eclipse.ui.OutputPage;
import org.apache.axis2.tool.codegen.eclipse.ui.ToolSelectionPage;
import org.apache.axis2.tool.codegen.eclipse.ui.WSDLFileSelectionPage;
import org.apache.axis2.tool.codegen.eclipse.util.SettingsConstants;
import org.apache.axis2.wsdl.codegen.CodeGenConfiguration;
import org.apache.axis2.wsdl.codegen.CodeGenerationEngine;
import org.apache.wsdl.WSDLDescription;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Vector;

/**
 * The main wizard for the codegen wizard
 */

public class CodeGenWizard extends Wizard implements INewWizard
{
   private ToolSelectionPage toolSelectionPage;  
   private WSDLFileSelectionPage wsdlSelectionPage;
   private OptionsPage optionsPage;
   private OutputPage outputPage;
   private JavaWSDLOptionsPage java2wsdlOptionsPage;
   private JavaSourceSelectionPage javaSourceSelectionPage;
   private JavaWSDLOutputLocationPage java2wsdlOutputLocationPage;
   
   private int selectedWizardType=SettingsConstants.WSDL_2_JAVA_TYPE;//TODO change this
   private ISelection selection;
   private boolean canFinish = false;

   /**
    * Constructor for CodeGenWizard.
    */
   public CodeGenWizard()
   {
      super();
      setNeedsProgressMonitor(true);
      this.setWindowTitle(org.apache.axis2.tool.codegen.eclipse.plugin.CodegenWizardPlugin
         .getResourceString("general.name"));
   }

   /**
    * Adding the page to the wizard.
    */

   public void addPages()
   {
      toolSelectionPage = new ToolSelectionPage();
      addPage(toolSelectionPage);
      
      //add the wsdl2java wizard pages
      wsdlSelectionPage = new WSDLFileSelectionPage();
      addPage(wsdlSelectionPage);
      optionsPage = new OptionsPage();
      addPage(optionsPage);
      outputPage = new OutputPage();
      addPage(outputPage);
      
      //add java2wsdl wizard pages
      javaSourceSelectionPage = new JavaSourceSelectionPage();
      addPage(javaSourceSelectionPage);
      java2wsdlOptionsPage = new JavaWSDLOptionsPage();
      addPage(java2wsdlOptionsPage);
      java2wsdlOutputLocationPage = new JavaWSDLOutputLocationPage();
      addPage(java2wsdlOutputLocationPage);
      

      

   }

   
    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.IWizard#canFinish()
     */
    public boolean canFinish() {
       IWizardPage[] pages = getPages();
       AbstractWizardPage wizardPage = null;
		for (int i = 0; i < pages.length; i++) {
		    wizardPage = (AbstractWizardPage)pages[i];
		    if (wizardPage.getPageType()==this.selectedWizardType){
		        if (!(wizardPage.isPageComplete()))
		            return false;
		    	}
		}
		return true;
    }
   public IWizardPage getNextPage(IWizardPage page) {
       AbstractWizardPage currentPage=(AbstractWizardPage)page;
       AbstractWizardPage pageout = (AbstractWizardPage)super.getNextPage(page);
       
       while (pageout!=null && selectedWizardType!=pageout.getPageType()){
           AbstractWizardPage temp = pageout;
           pageout = (AbstractWizardPage)super.getNextPage(currentPage);
           currentPage = temp;
           
       }
       return pageout;
    }
  
   /**
    * This method is called when 'Finish' button is pressed in the wizard. We will create an operation and run it using
    * wizard as execution context.
    */
   public boolean performFinish()
   {
      try
      {
        switch (selectedWizardType){
         case SettingsConstants.WSDL_2_JAVA_TYPE:doFinishWSDL2Java();break;
         case SettingsConstants.JAVA_2_WSDL_TYPE:doFinishJava2WSDL();break;
         case SettingsConstants.UNSPECIFIED_TYPE:break; //Do nothing
         default:throw new RuntimeException("Invalid state!");
        }
      }
      catch (Exception e)
      {
         MessageDialog.openError(getShell(), CodegenWizardPlugin.getResourceString("general.Error"), e.getMessage());
         return false;
      }
      MessageDialog.openInformation(this.getShell(), CodegenWizardPlugin.getResourceString("general.name"),
         CodegenWizardPlugin.getResourceString("wizard.success"));
      return true;
   }
   

   /**
    * The worker method, generates the code itself.
    */
   private void doFinishWSDL2Java()
   {

      WorkspaceModifyOperation op = new WorkspaceModifyOperation()
      {
         protected void execute(IProgressMonitor monitor)
         {
            if (monitor == null)
               monitor = new NullProgressMonitor();

            /*
             * "3" is the total amount of steps, see below monitor.worked(amount)
             */
            monitor.beginTask(CodegenWizardPlugin.getResourceString("generator.generating"), 3);

            try
            {
               /*
                * TODO: Introduce a progress monitor interface for CodeGenerationEngine.
                * Since this monitor here doesn't make much sense, we
                * should either remove the progress monitor from the CodeGenWizard,
                * or give a (custom) progress monitor to the generate() method, so
                * we will be informed by Axis2 about the progress of code generation.  
                */
               WSDL2JavaGenerator generator = new WSDL2JavaGenerator(); 
               monitor.subTask(CodegenWizardPlugin.getResourceString("generator.readingWOM"));
               WSDLDescription wom = generator.getWOM(wsdlSelectionPage.getFileName());
               monitor.worked(1);
               
               Map optionsMap = generator.fillOptionMap(optionsPage.isAsyncOnlyOn(),
                       									optionsPage.isSyncOnlyOn(),
                       									optionsPage.isServerside(),
                       									optionsPage.isServerXML(),
                       									optionsPage.isGenerateTestCase(),
                       									wsdlSelectionPage.getFileName(),
                       									optionsPage.getPackageName(),
                       									optionsPage.getSelectedLanguage(),
                       									outputPage.getOutputLocation());
               CodeGenConfiguration codegenConfig = new CodeGenConfiguration(wom, optionsMap);
               monitor.worked(1);
               
               monitor.subTask(CodegenWizardPlugin.getResourceString("generator.generating"));
               new CodeGenerationEngine(codegenConfig).generate();
               monitor.worked(1);
            }
            catch (Exception e)
            {
               e.printStackTrace(); 
               throw new RuntimeException(e);
            }

            monitor.done();
         }
      };


      /*
       * Start the generation as new Workbench Operation, so the user
       * can see the progress and, if needed, can stop the operation.
       */
      try
      {
         getContainer().run(false, true, op);
      }
      catch (InvocationTargetException e1)
      {
          throw new RuntimeException(e1);
      }
      catch (InterruptedException e1)
      {
         throw new RuntimeException("User Aborted!");
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }
   
   private void doFinishJava2WSDL() throws Exception{
           
           WorkspaceModifyOperation op = new WorkspaceModifyOperation()
           {
              protected void execute(IProgressMonitor monitor)
              {
                 if (monitor == null)
                    monitor = new NullProgressMonitor();

                 /*
                  * "2" is the total amount of steps, see below monitor.worked(amount)
                  */
                 monitor.beginTask(CodegenWizardPlugin.getResourceString("generator.generating"), 2);

                 try
                 {
                     monitor.worked(1);
                    String classLocation = javaSourceSelectionPage.getClassLocation();
                    monitor.worked(1);
                    String className = javaSourceSelectionPage.getClassName();
                    monitor.worked(1);
                    String locationURL = java2wsdlOptionsPage.getLocationURL();
                    monitor.worked(1);
                    String inputWSDLName = java2wsdlOptionsPage.getInputWSDLName();
                    monitor.worked(1);
                    String bindingName = java2wsdlOptionsPage.getBindingName();
                    monitor.worked(1);
                    String portypeName = java2wsdlOptionsPage.getPortypeName();
                    monitor.worked(1);
                    String style = java2wsdlOptionsPage.getStyle();
                    monitor.worked(1);
                    String fullFileName = java2wsdlOutputLocationPage.getFullFileName();
                    monitor.worked(1);
                    int mode = java2wsdlOptionsPage.getMode();
                    monitor.worked(1);
                    Vector selectedMethods = javaSourceSelectionPage.getSelectedMethods();
                    monitor.worked(1);
                    new Java2WSDLGenerator().emit(
                             classLocation,
                             className,
                             locationURL,
                             inputWSDLName,
                             bindingName,
                             portypeName,
                             style,
                             fullFileName,
                             mode,
                             selectedMethods
                     );
                     monitor.worked(1);
                 }
                 catch (Throwable e)
                 {
                    throw new RuntimeException(e);
                 }

                 monitor.done();
              }
           };
         
           try
           {
              getContainer().run(false, true, op);
           }
           catch (InvocationTargetException e1)
           {
               throw new RuntimeException(e1);
           }
           catch (InterruptedException e1)
           {
              throw new RuntimeException("User Aborted!");
           }
           catch (Exception e)
           {
              throw new RuntimeException(e);
           } 
   
   }

  

   /**
    * We will accept the selection in the workbench to see if we can initialize from it.
    * 
    * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
    */
   public void init(IWorkbench workbench, IStructuredSelection selection)
   {
      this.selection = selection;
   }

   
   
   
/**
 * @return Returns the selectedWizardType.
 */
public int getSelectedWizardType() {
    return selectedWizardType;
}
/**
 * @param selectedWizardType The selectedWizardType to set.
 */
public void setSelectedWizardType(int selectedWizardType) {
    this.selectedWizardType = selectedWizardType;
}
}