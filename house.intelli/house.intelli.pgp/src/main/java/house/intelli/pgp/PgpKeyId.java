package house.intelli.pgp;

import static java.util.Objects.*;
import static house.intelli.core.util.HashUtil.*;
import static house.intelli.core.util.IOUtil.*;

import java.io.Serializable;
import java.lang.ref.WeakReference;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import house.intelli.pgp.jaxb.PgpKeyIdXmlAdapter;

@XmlJavaTypeAdapter(value=PgpKeyIdXmlAdapter.class)
public class PgpKeyId implements Comparable<PgpKeyId>, Serializable {
	private static final long serialVersionUID = 1L;

	private final long pgpKeyId;
	private transient WeakReference<String> toString;
	private transient WeakReference<String> toHumanString;

	public PgpKeyId(final long pgpKeyId) {
		this.pgpKeyId = pgpKeyId;
	}

	public PgpKeyId(final String pgpKeyIdString) {
		this(bytesToLong(decodeHexStr(requireNonNull(pgpKeyIdString, "pgpKeyIdString"))));
	}

	@Override
	public String toString() {
		String s = toString == null ? null : toString.get();
		if (s == null) {
			s = encodeHexStr(longToBytes(pgpKeyId));
			toString = new WeakReference<String>(s);
		}
		return s;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (pgpKeyId ^ (pgpKeyId >>> 32));
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final PgpKeyId other = (PgpKeyId) obj;
		return this.pgpKeyId == other.pgpKeyId;
	}

	@Override
	public int compareTo(PgpKeyId other) {
		requireNonNull(other, "other");
		// Same semantics as for normal numbers.
		return (this.pgpKeyId < other.pgpKeyId ? -1 :
				(this.pgpKeyId > other.pgpKeyId ? 1 : 0));
	}

	public long longValue() {
		return pgpKeyId;
	}

	public String toHumanString() {
		String s = toHumanString == null ? null : toHumanString.get();
		if (s == null) {
			s = _toHumanString();
			toHumanString = new WeakReference<String>(s);
		}
		return s;
	}

	private String _toHumanString() {
		final StringBuilder sb = new StringBuilder();
		final String string = toString();

		for (int i = 0; i < string.length(); ++i) {
			if (i > 0 && (i % 4 == 0))
				sb.append(':');

			sb.append(string.charAt(i));
		}
		return sb.toString();
	}
}
