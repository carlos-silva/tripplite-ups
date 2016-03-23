package octopus.ups;

/**
 * Provides generic functions that should be implemented by any UPS. Add more functions as needed.
 * The functions may not work for all UPS types. Each implementing class must define the following 
 * functions and provide results accordingly.
 */
public interface UPS{
	/**
	 * Provides interface to turn off a load on UPS.
	 */
	public boolean turnOffLoad(String x);
	/**
	 * Provides interface to turn off all (controllable) loads on UPS.
	 */
	public boolean turnOffAllLoads();
	/**
	 * Provides an interface to turn on a load on UPS.
	 */
	public boolean turnOnLoad(String x);
	/**
	 * Provides interface to turn on all (controllable) loads on UPS.
	 */
	public boolean turnOnAllLoads();
	/**
	 * Provides an interface to determine if load is on.
	 */
	public boolean isLoadOn(String x);
	/**
	 * Provides an method to get provided load status.
	 */
	public String getLoadStatus(String x);
	/**
	 * Provides an method to get battery charge.
	 */
	public String getBatteryCharge();
	/**
	 * Provides a method to get current temperature.
	 */
	public String getTemperature();
	/**
	 * Returns the current input voltage
	 */
	public String getInputVoltage();
	/**
	 * Returns the current output voltage
	 */
	public String getOutputVoltage();
	/**
	 * Returns the current ouput load
	 */
	public String getLoad();
	/**
	 * Returns the UPS model.
	 */
	public String getModel();
	/**
	 * Returns the UPS hostname.
	 */
	public String getHostname();
	/**
	 * Returns the UPS IP address.
	 */
	public String getIP();
	/**
	 * Returns the UPS ID number.
	 */
	public int getID();
	/**
	 * Returns the number of controllable loads.
	 */
	public int getControllableLoadCount();
	/**
	 * Returns true if the battery is reported as good by UPS.
	 */
	public boolean hasGoodBattery();
}
