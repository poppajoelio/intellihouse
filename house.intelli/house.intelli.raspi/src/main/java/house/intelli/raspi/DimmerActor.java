package house.intelli.raspi;

import static house.intelli.core.event.EventQueue.*;
import static house.intelli.core.util.AssertUtil.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinState;
import com.pi4j.wiringpi.Gpio;

import house.intelli.core.bean.AbstractBean;
import house.intelli.core.bean.PropertyBase;

public class DimmerActor extends AbstractBean<DimmerActor.Property> implements AutoCloseable {
	private final Logger logger = LoggerFactory.getLogger(DimmerActor.class);

	public static interface Property extends PropertyBase { }

	public static enum PropertyEnum implements Property {
		pin,
		dimmerValue
	}

	public static final int MIN_DIMMER_VALUE = 0;
	public static final int MAX_DIMMER_VALUE = 100;
	private static final boolean USE_DIGITAL_OUTPUT_FOR_EXTREME_VALUES = false;

	private Pin pin;
	private GpioPinPwmOutput pwmOutput;
	private GpioPinDigitalOutput digitalOutput;
	private int dimmerValue = MIN_DIMMER_VALUE;

	public void init() {
		assertEventThread();
		assertNotNull(pin, "pin");
		applyDimmerValue();
	}

	public Pin getPin() {
		return pin;
	}
	public void setPin(Pin pin) {
		if (this.pin != null)
			throw new IllegalStateException("pin already assigned!");

		assertEventThread();
		setPropertyValue(PropertyEnum.pin, pin);
	}

	public int getDimmerValue() {
		assertEventThread();
		return dimmerValue;
	}
	public void setDimmerValue(int dimmerValue) { // not synchronized to prevent deadlocks in listeners
		if (dimmerValue < MIN_DIMMER_VALUE)
			throw new IllegalArgumentException("dimmerValue < MIN_DIMMER_VALUE");

		if (dimmerValue > MAX_DIMMER_VALUE)
			throw new IllegalArgumentException("dimmerValue > MAX_DIMMER_VALUE");

		assertEventThread();

		if (setPropertyValue(PropertyEnum.dimmerValue, dimmerValue))
			applyDimmerValue();
	}

	protected void applyDimmerValue() {
		if (USE_DIGITAL_OUTPUT_FOR_EXTREME_VALUES && (dimmerValue == MIN_DIMMER_VALUE || dimmerValue == MAX_DIMMER_VALUE)) {
			openDigitalOutput();
			if (dimmerValue == MAX_DIMMER_VALUE)
				digitalOutput.setState(PinState.HIGH);
			else
				digitalOutput.setState(PinState.LOW);
		}
		else {
			openPwmOutput();
			pwmOutput.setPwm(getPwm());
		}
	}

	private int getPwm() {
//		if (pwmOutput.isMode(PinMode.PWM_OUTPUT))
//			return dimmerValue * 512 / 100; // is this really true? in my tests, it didn't look like the logical pwm value was really this - it looked like being 0 to 100, too, even for hardware-pwm.

		return dimmerValue;
	}

	private void openPwmOutput() {
		assertEventThread();
		if (pwmOutput != null)
			return;

		assertNotNull(pin, "pin");

		final boolean hardPwm = pin.getSupportedPinModes().contains(PinMode.PWM_OUTPUT);
		logger.debug("openPwmOutput: hardPwm={}", hardPwm);

		closeDigitalOutput();

		GpioController gpioController = GpioFactory.getInstance();

		if (hardPwm) {
			pwmOutput = gpioController.provisionPwmOutputPin(pin);
			Gpio.pwmSetMode(Gpio.PWM_MODE_BAL);
			Gpio.pwmSetClock(1920);
			Gpio.pwmSetRange(100);
		}
		else {
			pwmOutput = gpioController.provisionSoftPwmOutputPin(pin);
		}
	}

	private void openDigitalOutput() {
		assertEventThread();
		if (digitalOutput != null)
			return;

		assertNotNull(pin, "pin");

		logger.debug("openDigitalOutput");

		closePwmOutput();

		GpioController gpioController = GpioFactory.getInstance();
		digitalOutput = gpioController.provisionDigitalOutputPin(pin);
	}

	private void closePwmOutput() {
		assertEventThread();
		if (pwmOutput == null)
			return;

		logger.debug("closePwmOutput");
		GpioController gpioController = GpioFactory.getInstance();
		gpioController.unprovisionPin(pwmOutput);
		pwmOutput = null;
	}

	private void closeDigitalOutput() {
		assertEventThread();
		if (digitalOutput == null)
			return;

		logger.debug("closeDigitalOutput");
		GpioController gpioController = GpioFactory.getInstance();
		gpioController.unprovisionPin(digitalOutput);
		digitalOutput = null;
	}

	@Override
	public void close() {
		logger.debug("close");
		invokeAndWait(new Runnable() {
			@Override
			public void run() {
				closeDigitalOutput();
				closePwmOutput();
			}
		});
	}
}