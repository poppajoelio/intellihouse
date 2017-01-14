package house.intelli.raspi;

import static house.intelli.core.event.EventQueue.*;
import static house.intelli.core.util.AssertUtil.*;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import house.intelli.core.bean.AbstractBean;
import house.intelli.core.bean.PropertyBase;

public class KeyButtonSensor extends AbstractBean<KeyButtonSensor.Property> implements AutoCloseable {

	public static interface Property extends PropertyBase { }

	public static enum PropertyEnum implements Property {
		pin,
		down
	}

	private Pin pin;
	private GpioPinDigitalInput digitalInput;

	private boolean down;

	private GpioPinListenerDigital listener = new GpioPinListenerDigital() {
		@Override
		public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
			final PinState state = event.getState();
			invokeLater(new Runnable() {
				@Override
				public void run() {
					_setDown(state);
				}
			});
		}
	};

	public Pin getPin() {
		return pin;
	}
	public void setPin(Pin pin) {
		if (this.pin != null)
			throw new IllegalStateException("pin already assigned!");

		assertEventThread();
		setPropertyValue(PropertyEnum.pin, pin);
	}

	public void init() {
		assertEventThread();
		assertNotNull(pin, "pin");
		openDigitalInput();
	}

	public boolean isDown() {
		return down;
	}

	protected void setDown(boolean down) {
		assertEventThread();
		setPropertyValue(PropertyEnum.down, down);
	}

	protected void _setDown(PinState pinState) {
		setDown(PinState.HIGH == pinState);
	}

	private void openDigitalInput() {
		if (digitalInput != null)
			return;

		assertEventThread();
		assertNotNull(pin, "pin");

		GpioController gpioController = GpioFactory.getInstance();
		digitalInput = gpioController.provisionDigitalInputPin(pin);
		digitalInput.setDebounce(50, PinState.HIGH);
		digitalInput.setDebounce(100, PinState.LOW);
		digitalInput.addListener(listener);
		_setDown(digitalInput.getState());
	}

	private void closeDigitalInput() {
		assertEventThread();
		if (digitalInput == null)
			return;

		digitalInput.removeListener(listener);
		GpioController gpioController = GpioFactory.getInstance();
		gpioController.unprovisionPin(digitalInput);
		digitalInput = null;
	}

	@Override
	public void close() {
		invokeAndWait(new Runnable() {
			@Override
			public void run() {
				closeDigitalInput();
			}
		});
	}
}
