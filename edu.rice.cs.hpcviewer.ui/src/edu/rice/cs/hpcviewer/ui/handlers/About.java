 
package edu.rice.cs.hpcviewer.ui.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IconAndMessageDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import edu.rice.cs.hpcviewer.ui.resources.IconManager;


/****
 * 
 * Class to show the About window.
 * Called only as a menu handler. Otherwise it doesn' work.
 *
 */
public class About 
{
	
	@Execute
	public void execute(@Active Shell shell) {
		
		AboutDialog dialog = new AboutDialog(shell);
		
		dialog.open();
	}
	
	
	/****
	 * 
	 * Main window to show the about dialog
	 *
	 */
	static class AboutDialog extends IconAndMessageDialog
	{
		// constants copied from org.eclipse.ui.branding.IProductConstants.java
		// we cannot directly import this interface because they are part of 
		// org.eclipse.ui bundle which will force us to use compatibility layer. 
		
		private static final String APP_NAME   = "appName";   //$NON-NLS-1$
		private static final String ABOUT_TEXT = "aboutText"; //$NON-NLS-1$
		private static final String FILE_VERSION = "platform:/plugin/edu.rice.cs.hpcviewer.ui/release.txt";
		private static final String FILE_LICENSE = "platform:/plugin/edu.rice.cs.hpcviewer.ui/License.txt";
		
		
		public AboutDialog(Shell parentShell) {
			super(parentShell);

			IProduct product = Platform.getProduct();
			this.message     = product.getProperty(ABOUT_TEXT);
			
			try {
				URL url = FileLocator.toFileURL(new URL(FILE_VERSION));
				String filePath = url.getFile();
				File file = new File(filePath);
				FileInputStream fis = new FileInputStream(file);
				byte[] data = new byte[(int) file.length()];
				fis.read(data);
				
				this.message += "\n\nRelease: " + new String(data, "UTF-8");
				
				fis.close();
				
			} catch (IOException e) {

				e.printStackTrace();
			}
		}

		@Override
		public Image getImage() {
			ImageRegistry registry = JFaceResources.getImageRegistry();
			Image image = registry.get(IconManager.Image_Viewer_64);
			
			if (image != null)
				return image;
			
			try {
				URL url = FileLocator.toFileURL(new URL(IconManager.Image_Viewer_64));
				image = new Image(Display.getDefault(), url.getFile());
				registry.put(IconManager.Image_Viewer_64, image);
				
				return image;
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
			createButton(parent, IDialogConstants.DETAILS_ID, "License", false);
		}
		
		
		@Override
		protected void buttonPressed(int buttonId) {
			if (buttonId == IDialogConstants.DETAILS_ID) {
				showLicense();
			}
			super.buttonPressed(buttonId);
		}
		
		@Override
		protected Control createDialogArea(Composite parent) {
			// create message area
			createMessageArea(parent);
			// create the top level composite for the dialog area
			Composite composite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			composite.setLayout(layout);
			GridData data = new GridData(GridData.FILL_BOTH);
			data.horizontalSpan = 2;
			composite.setLayoutData(data);

			return composite;
		}
		

		@Override
		protected void configureShell(Shell shell) {
			super.configureShell(shell);

			IProduct product = Platform.getProduct();
			String title     = product.getProperty(APP_NAME);

			if (title != null) {
				shell.setText(title);
			}
		}

		private void showLicense() {
			
			try {
				URL url = FileLocator.toFileURL(new URL(FILE_LICENSE));
				String filePath = url.getFile();
				File file = new File(filePath);
				FileInputStream fis = new FileInputStream(file);
				byte[] data = new byte[(int) file.length()];
				fis.read(data);
				
				String license = new String(data, "UTF-8");				
				fis.close();
				
				MessageDialog.openInformation(getShell(), "License", license);

				
			} catch (IOException e) {

				e.printStackTrace();
			}
		}
	}
}