package org.hawk.core.query;

import java.util.Set;

public interface IAccessListener {

	public void setSourceObject(String s);

	public void accessed(String accessObject, String property);

	public Set<IAccess> getAccesses();

	public void resetAccesses();

	public void removeAccess(IAccess a);

}
