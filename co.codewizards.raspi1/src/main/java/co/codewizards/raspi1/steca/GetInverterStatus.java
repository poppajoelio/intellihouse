package co.codewizards.raspi1.steca;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.raspi1.steca.dto.InverterStatus;

public class GetInverterStatus extends StecaRequest<InverterStatus> {

	private static final Logger logger = LoggerFactory.getLogger(GetInverterStatus.class);

	private static final byte[] COMMAND = new byte[] {
			'Q', 'P', 'I', 'G', 'S',
			(byte) 0xB7, (byte) 0xA9, // CRC
			'\r'
	};

	public GetInverterStatus() {
	}

	@Override
	public InverterStatus execute() throws IOException {
		final OutputStream out = getStecaClientOrFail().getOutputStream();
		out.write(COMMAND);

		final byte[] response = readResponse();

		String s = new String(response, StandardCharsets.US_ASCII);
		String[] fields = s.split(" ");
		if (fields.length != 21)
			throw new IOException("Malformed response: " + s);

//		System.out.println(s);
//
//		for (int i = 0; i < fields.length; i++) {
//			System.out.println(Integer.toString(i) + ": " + fields[i]);
//		}

		InverterStatus result = new InverterStatus();

		int idx = -1;
		result.setAcInVoltage(parseFloat(fields[++idx]));                    //  0
		result.setAcInFrequency(parseFloat(fields[++idx]));                  //  1
		result.setAcOutVoltage(parseFloat(fields[++idx]));                   //  2
		result.setAcOutFrequency(parseFloat(fields[++idx]));                 //  3
		result.setAcOutApparentPower(parseFloat(fields[++idx]));             //  4
		result.setAcOutActivePower(parseFloat(fields[++idx]));               //  5
		result.setAcOutLoadPercentage(parseFloat(fields[++idx]));            //  6
		result.setInternalBusVoltage(parseFloat(fields[++idx]));             //  7
		result.setBatteryVoltageAtInverter(parseFloat(fields[++idx]));       //  8
		result.setBatteryChargeCurrent(parseFloat(fields[++idx]));           //  9
		result.setBatteryCapacityPercentage(parseFloat(fields[++idx]));      // 10
		result.setHeatSinkTemperature(parseFloat(fields[++idx]));            // 11
		result.setPvToBatteryCurrent(parseInt(fields[++idx]));               // 12
		result.setPvVoltage(parseFloat(fields[++idx]));                      // 13
		result.setBatteryVoltageAtCharger(parseFloat(fields[++idx]));        // 14
		result.setBatteryDischargeCurrent(parseFloat(fields[++idx]));        // 15
		result.setStatusBitmask(parseIntBinary(fields[++idx]));              // 16
		++idx; // reserved field                                             // 17
		result.setEepromVersion(parseInt(fields[++idx]));                    // 18
		result.setPvChargePower(parseFloat(fields[++idx]));                  // 19

		int statusBits2 = parseIntBinary(fields[++idx]);                     // 20
		statusBits2 = statusBits2 << 8;
		result.setStatusBitmask(result.getStatusBitmask() | statusBits2);

		return result;
	}

	public static void main(String[] args) {
		System.out.println(parseInt("01084"));
		System.out.println(parseInt("01084"));
	}
}
