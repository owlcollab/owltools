package owltools.gaf.lego.json;

import java.util.Arrays;


abstract class JsonAnnotatedObject {
	public JsonAnnotation[] annotations;

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(annotations);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JsonAnnotatedObject other = (JsonAnnotatedObject) obj;
		if (!Arrays.equals(annotations, other.annotations)) {
			return false;
		}
		return true;
	}

}