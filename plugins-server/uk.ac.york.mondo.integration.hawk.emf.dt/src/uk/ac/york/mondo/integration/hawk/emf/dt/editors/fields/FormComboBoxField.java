package uk.ac.york.mondo.integration.hawk.emf.dt.editors.fields;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;

/**
 * Combo box field that takes up two columns in the grid.
 */
public class FormComboBoxField {
	private final Label label;
	private final Combo combobox;

	public FormComboBoxField(FormToolkit toolkit, Composite sectionClient, String labelText, String[] options) {
	    label = toolkit.createLabel(sectionClient, labelText, SWT.WRAP);
	    label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

	    final TableWrapData layoutData = new TableWrapData();
		layoutData.valign = TableWrapData.MIDDLE;
		label.setLayoutData(layoutData);

		combobox = new Combo(sectionClient, SWT.READ_ONLY);
		combobox.setItems(options);
		combobox.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
	}

	public Combo getCombo() {
		return combobox;
	}
}