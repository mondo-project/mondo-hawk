package uk.ac.york.mondo.integration.hawk.emf.dt.editors;

import org.eclipse.ui.forms.widgets.TableWrapLayout;

/**
 * Common utility methods.
 */
class Utils {

	private Utils() {}

	public static String concat(final String[] elems, final String separator) {
		final StringBuffer sbuf = new StringBuffer();
		boolean bFirst = true;
		for (String filePattern : elems) {
			if (bFirst) {
				bFirst = false;
			} else {
				sbuf.append(separator);
			}
			sbuf.append(filePattern);
		}
		return sbuf.toString();
	}

	public static TableWrapLayout createTableWrapLayout(int nColumns) {
		final TableWrapLayout cContentsLayout = new TableWrapLayout();
	    cContentsLayout.numColumns = nColumns;
	    cContentsLayout.horizontalSpacing = 5;
	    cContentsLayout.verticalSpacing = 3;
		return cContentsLayout;
	}
}
