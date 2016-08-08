package org.hawk.service.emf.dt.editors.fields;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;

/**
 * Paired label and text field.
 */
public class FormTextField {
	private final FormText label;
	private final Text text;

	public FormTextField(FormToolkit toolkit, Composite sectionClient, String labelText, String defaultValue) {
		this(toolkit, sectionClient, labelText, defaultValue, SWT.BORDER|SWT.WRAP);
	}

	public FormTextField(FormToolkit toolkit, Composite sectionClient, String labelText, String defaultValue, int textStyle) {
	    label = toolkit.createFormText(sectionClient, true);
	    label.setText("<form><p>" + labelText + "</p></form>", true, false);
	    label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

	    final TableWrapData layoutData = new TableWrapData();
		layoutData.valign = TableWrapData.MIDDLE;
		label.setLayoutData(layoutData);

		text = toolkit.createText(sectionClient, defaultValue, textStyle|SWT.WRAP);
		text.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
	}

	public Text getText() {
		return text;
	}

	public void setTextWithoutListener(String newText, ModifyListener disabledListener) {
		text.removeModifyListener(disabledListener);
		text.setText(newText);
		text.addModifyListener(disabledListener);
	}

	public FormText getLabel() {
		return label;
	}
}