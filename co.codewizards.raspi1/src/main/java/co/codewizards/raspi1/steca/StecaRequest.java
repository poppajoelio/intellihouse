package co.codewizards.raspi1.steca;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public abstract class StecaRequest<R> extends AbstractRequest<R> {

	private static final long readTimeoutMillis = 3000;

	protected static int parseIntBinary(String string) {
		return Integer.parseInt(string, 2);
	}

	protected static int parseInt(String string) {
		return Integer.parseInt(string);
	}

	protected static float parseFloat(String string) {
		return Float.parseFloat(string);
	}

	/**
	 * Reads a response via {@link #readRawResponse()} and then extracts the payload, verifies it via CRC
	 * and returns this payload, if it is OK.
	 * @return the payload-response. Never <code>null</code>.
	 * @throws IOException if reading failed.
	 */
	protected byte[] readResponse() throws IOException {
		final byte[] rawResponse = readRawResponse();

		if (rawResponse.length < 4)
			throw new IOException("Response too short: " + Arrays.toString(rawResponse));

		if (rawResponse[0] != '(')
			throw new IOException("Response did not start with '(': " + Arrays.toString(rawResponse));

		final byte[] response = new byte[rawResponse.length - 1 - 2 - 1]; // - starter '(' - CRC - \r
		System.arraycopy(rawResponse, 1, response, 0, response.length);

		final Crc crc = new Crc();
		crc.update((byte) '('); // first char from raw response, not contained in response, anymore
		crc.update(response);

		final byte[] crcBytes = crc.getCrcBytes();
		int rawResponseCrcIndex = rawResponse.length - 1; // at \r

		--rawResponseCrcIndex; // at 2nd CRC byte
		if (crcBytes[1] != rawResponse[rawResponseCrcIndex])
			throw new IOException(String.format("CRC error! calculated=%s found=%s",
					toHex(crcBytes[0]) + toHex(crcBytes[1]),
					toHex(rawResponse[rawResponseCrcIndex - 1]) + toHex(rawResponse[rawResponseCrcIndex])));

		--rawResponseCrcIndex; // at 1st CRC byte
		if (crcBytes[0] != rawResponse[rawResponseCrcIndex])
			throw new IOException(String.format("CRC error! calculated=%s found=%s",
					toHex(crcBytes[0]) + toHex(crcBytes[1]),
					toHex(rawResponse[rawResponseCrcIndex]) + toHex(rawResponse[rawResponseCrcIndex + 1])));

		return response;
	}

	protected static String toHex(final byte val) {
		String hex = Integer.toHexString(val & 0xff);
		if (hex.length() < 2)
			hex = "0" + hex;

		return hex;
	}

	/**
	 * Reads a raw response (until CR) in a non-blocking way and taking the timeout {@link #readTimeoutMillis} into account.
	 * @return the response (until and including the CR).
	 * @throws IOException if reading failed.
	 */
	protected byte[] readRawResponse() throws IOException {
		final InputStream in = getStecaClientOrFail().getInputStream();

		final byte[] buf = new byte[1024];
		final ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();

		final long startTimeStamp = System.currentTimeMillis();
		while (true) {
			if (System.currentTimeMillis() - startTimeStamp > readTimeoutMillis)
				throw new IOException("Timeout! Read so far: " + Arrays.toString(responseBuffer.toByteArray()));

			final int available = in.available();
			if (available > 0) {
				final int length = in.read(buf);
				responseBuffer.write(buf, 0, length);
				if (containsCr(buf, length))
					return responseBuffer.toByteArray();
			}
			else {
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
	}

	protected boolean containsCr(final byte[] buf, final int length) {
		for (int i = 0; i < buf.length; i++) {
			if (i >= length)
				return false;

			if (buf[i] == '\r')
				return true;
		}
		return false;
	}

}
