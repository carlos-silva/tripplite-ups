package octopus;

import octopus.ups.*;
import octopus.utils.snmp.*;
import com.adventnet.snmp.snmp2.*;
/**
 * This class provides methods to interact with Tripplite UPS via SNMP commands. 
 * The class will be updated with new functions as needed. The goal is to provide a 
 * stand-alone package that can be incorporated to Java applications requiring 
 * interaction with UPS, thus, it implements the UPS interface so applications can 
 * be designed to work with "any" UPS rather than Tripplite UPS.
 */
public class TrippLite implements UPS {
	boolean debug;
	String ip_address;
	String hostname;
	String model;
	OctopusSnmpClient snmp;	
	int id;

	// number of loads on UPS -- in future determine by OID
	String [] loads = {
		"1",
		"2"
	};
	// number of seconds to sleep in between shutdown/power on of UPS loads
	int load_wait = 20;
	long load_wait_millisec = load_wait * 1000;

	// list of oids that are used by TrippLite UPS
	// set what load to turn off/on
	// turn off/on load
	String [] control_load_oid = {
		".1.3.6.1.4.1.850.100.1.10.2.1.4.1",
		".1.3.6.1.4.1.850.100.1.10.2.1.4.2"
	};
	String [] load_status_oid = {
		".1.3.6.1.4.1.850.100.1.10.2.1.2.1",
		".1.3.6.1.4.1.850.100.1.10.2.1.2.2"
	};
	// battery charge
	String battery_charge_oid = ".1.3.6.1.2.1.33.1.2.4.0";
	// temperature in Farenheit
	String temperature_oid = ".1.3.6.1.4.1.850.100.1.2.2.0";
	// input voltage
	String input_volt_oid =  ".1.3.6.1.2.1.33.1.3.3.1.3.1";
	// output voltage
	String output_volt_oid = ".1.3.6.1.2.1.33.1.4.4.1.2.1";
	// output load (in percent)
	String output_load_oid = ".1.3.6.1.2.1.33.1.4.4.1.5";
	// get battery condition
	String battery_status_oid = ".1.3.6.1.2.1.33.1.2.1.0";
	// status of ups off/on
	String load_off = "1";
	String load_on = "2";
	// battery status
	final String battery_good_str = "2";

	/**
	 * Constructor class that instantiates a TrippLite object. 
	 * @param ip_address String representation of the IP address.
	 * @param hostname String consisting of the device hostname, e.g. UPS00_1_1
	 * @param model <code>String</code> object representing the UPS model
	 * @param community <code>String</code> object representing the community string
	 * @param version <code>int</code> object representing the SNMP version. Possible values are 0=SNMPv1, 1=SNMPv2c, 2=SNMPv3.
	 */
	public TrippLite(String ip_address, String hostname, String model, int id, String community, int version){
		this.ip_address = ip_address;
		this.hostname = hostname;
		this.model = model;
		this.id = id;
		this.debug = false;

		// create the OctopusSnmpClient
		snmp = new OctopusSnmpClient(ip_address, community, version);		
	}
	/**
	 * Constructor class that instantiates a TrippLite object. 
	 * @param ip_address String representation of the IP address.
	 * @param hostname String consisting of the device hostname, e.g. UPS00_1_1
	 * @param model <code>String</code> object representing the UPS model
	 * @param community <code>String</code> object representing the community string
	 * @param version <code>int</code> object representing the SNMP version. Possible values are 0=SNMPv1, 1=SNMPv2c, 2=SNMPv3.
	 * @param debug <code>boolean</code> flag to enable disable debugging.
	 */
	public TrippLite(String ip_address, String hostname, String model, int id, String community, int version, boolean debug){
		this.ip_address = ip_address;
		this.hostname = hostname;
		this.model = model;
		this.id = id;
		this.debug = debug;

		// create the OctopusSnmpClient
		snmp = new OctopusSnmpClient(ip_address, community, version, debug);
	}
	/** 
	 * Sends SNMP command to shutdown provided UPS load. In case of error, messages are printed to error buffer.
	 * @param x <code>String</code> representing the load number to turn off.
	 * @return <code>true</code> if the command is successfully executed. <code>false</code> if the command fails to execute.
	 */
	public boolean turnOffLoad(String x){
		if(x.compareTo("1") != 0 && x.compareTo("2") != 0){
			// return error, must be load 1 or load 2
			System.err.println("[TrippLite] ["+ip_address+"] Error: Load must be either 1 or 2! load="+x);
			return false;
		}
		// parse the integer value
		int i=-1;
		try{
			i=Integer.parseInt(x);
		}catch(NumberFormatException e){
			// wrong number format
			System.err.println("[TrippLite] ["+ip_address+"] Error: Unable to parse number '"+x+"':"+e.getMessage());
			return false;
		}
		if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Turning off load load="+x+", oid="+control_load_oid[i-1]);
		// turn off the provided bank
		String status = snmp.set(control_load_oid[i-1], SnmpAPI.INTEGER, load_off);
		// if null, then error occurred
		if(status == null){
			if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Set operation failed.");
			return false;
		}
		if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Command execution to turn off the load was successful.");
		return true;
	}
	/** 
	 * Sends commands to shutdown all controllable UPS loads. Returns true if all UPS loads were shutdown, false if otherwise.
	 * @return <code>true</code> if the command is successfully executed. <code>false</code> if the command fails to execute.
	 */
	public boolean turnOffAllLoads(){
		if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Turning off all loads");
		boolean success=true;
		for(int i=0; i<loads.length; i++){
			if(!turnOffLoad(loads[i])){
				// failure
				success=false;
			}
			// wait for before moving to next load
			try{
				Thread.sleep(load_wait_millisec);
			}catch(InterruptedException e){
				System.out.println("[TrippLite] ["+ip_address+"] Error: Interrupted while waiting to turn off next load");
			}
		}
		return success;
	}
	/**
	 * Sends SNMP command to turn on the provided UPS load. In case of error, messages are printed to error buffer.
	 * @param x <code>String</code> representing the load number to turn on.
	 * @return <code>true</code> if the command is successfully executed. <code>false</code> if the command fails to execute.
	 */
	public boolean turnOnLoad(String x){
		if(x == null) return false;
		// parse the load number
		int i=-1;
		try {
			i=Integer.parseInt(x);
		} catch(NumberFormatException e){
			// wrong number format
			System.err.println("[TrippLite] ["+ip_address+"] Error: Unable to parse number '"+x+"':"+e.getMessage());
			return false;
		}
		if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Turning on load="+x+", oid="+control_load_oid[i-1]);
		// turn on the provided bank
		String status = snmp.set(control_load_oid[i-1], SnmpAPI.INTEGER, load_on);
		// if null, then error occurred
		if(status == null){
			if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Command execution to turn on the load failed!!!");
			return false;
		}
		if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Command execution to turn on the load succeeded!!!");
		return true;
	}
	/** 
	 * Sends commands to turn on all controllable UPS loads. Returns true if all UPS loads were turned on, false if otherwise.
	 * @return <code>true</code> if the command is successfully executed. <code>false</code> if the command fails to execute.
	 */
	public boolean turnOnAllLoads(){
		if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Turning on all loads");
		boolean success=true;
		for(int i=0; i<loads.length; i++){
			if(!turnOnLoad(loads[i])){
				// failure
				success=false;
			}
			// wait before moving to next load
			try{
				Thread.sleep(load_wait_millisec);
			}catch(InterruptedException e){
				System.out.println("[TrippLite] ["+ip_address+"] Error: Interrupted while waiting to turn off next load");
			}
		}
		boolean loadOff=true;
		return success;
	}
	/**
	 * Return the load status of given load number.
	 * @param x <code>String</code> representing the load to return status on.
	 * @return Result of the SnmpTarget.get() request. <code>null</code> if error.
	 */
	public String getLoadStatus(String x){
		// returns the status of the provided load number
		if(x.compareTo("1") != 0 && x.compareTo("2") != 0){
			// invalid load number, return null
			System.err.println("[TrippLite] ["+ip_address+"] Error: Load must be either 1 or 2! load="+x);
			return null;
		}
		// determine if this is the correct number
		int i;
		try{
			i = Integer.parseInt(x);
		}catch(NumberFormatException e){
			// wrong number format
			System.err.println("[TrippLite] ["+ip_address+"] Error: Unable to parse number '"+x+"':"+e.getMessage());
			return null;
		}
		if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Getting status of load="+x+", oid="+load_status_oid[i-1]);
		// return the query
		return snmp.get(load_status_oid[i-1]);
	}
	/**
	 * Determine if the load is on/off.
	 * @param x <code>String</code> representing the load number to query if status is on.
	 * @return <code>true</code> if the load returns on state. <code>false</code> if the load returns off state or error.
	 */
	public boolean isLoadOn(String x){
		// determine if the load is on
		String status = getLoadStatus(x);
		if(debug) System.out.print("[TrippLite] ["+ip_address+"] Debug: Is load_status="+status+" eq load_on="+load_on+"? ");
		if(status.compareTo(load_on) == 0){
			if(debug) System.out.println("Yes");
			// load is on
			return true;
		}
		else{
			if(debug) System.out.println("No");
			return false;
		}
	}
	/**
	 * Returns the percentage of battery charge.
	 * @return <code>String</code> containing UPS model information. <code>null</code> if error.
	 */
	public String getBatteryCharge(){
		if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Get current battery charge oid="+battery_charge_oid);
		String status = snmp.get(battery_charge_oid);
		if(status == null){
			// an error occurred
			if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Unable to get the battery charge!!!");
		}
		// else, no error, return the current battery charge (percent)
		return status;
	}
	/**
	 * Returns the current temperature.
	 * @return <code>String</code> containing UPS temperature. <code>null</code> if error.
	 */
	public String getTemperature(){
		if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Get current temperature oid="+temperature_oid);
		String status = snmp.get(temperature_oid);
		if(status == null){
			// an error occurred
			if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Unable to get the current temperature!!!");
		}
		// else, no error, return the current temperature (in Farenheit)
		return status;
	}
	/**
	 * Returns the current input voltage.
	 */
	public String getInputVoltage(){
		if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Get current input voltage oid="+input_volt_oid);
		String status = snmp.get(input_volt_oid);
		if(status == null){
			// an error occurred
			if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Unable to get the current input voltage!!!");
		}
		// else, no error, return the current input voltage
		return status;
	}
	/**
	 * Returns the current input voltage.
	 */
	public String getOutputVoltage(){
		if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Get current output voltage oid="+output_volt_oid);
		String status = snmp.get(output_volt_oid);
		if(status == null){
			// an error occurred
			if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Unable to get the current output voltage!!!");
		}
		// else, no error, return the current input voltage
		return status;
	}
	/**
	 * Returns the current output load.
	 */
	public String getLoad(){
		if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Get current output load oid="+output_load_oid);
		String status = snmp.get(output_load_oid);
		if(status == null){
			// an error occurred
			if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Unable to get the current output load!!!");
		}
		// else, no error, return the current input voltage
		return status;
	}
	/**
	 * Returns the model information for this TrippLite object.
	 * @return <code>String</code> containing UPS model information.
	 */
	public String getModel(){
		// return the model of this ups
		return model;
	}
	/**
	 * Returns the hostname for this TrippLite object.
	 * @return <code>String</code> containing the hostname of the UPS.
	 */
	public String getHostname(){
		// return the hostname
		return hostname;
	}
	/**
	 * Returns the IP address for this TrippLite object.
	 * @return <code>String</code> containing the IP address of the UPS.
	 */
	public String getIP(){
		// return the IP address of the UPS
		return ip_address;
	}
	/**
	 * Returns the ID number assigned to this UPS.
	 * @return <code>int</code> containing the ID number of the UPS.
	 */
	public int getID(){
		// return the IP address of the UPS
		return id;
	}
	/**
	 * Returns the number of controllable loads.
	 * @return <code>int</code> representing the number of controllable loads (banks).
	 */
	public int getControllableLoadCount(){
		return loads.length;
	}
	/**
	 * Returns true if the UPS battery is reported good.
	 * @return <code>true</code> if the UPS battery is good, false if otherwise
	 */
	public boolean hasGoodBattery(){
		boolean battery_good=true;
		boolean battery_bad=false;
		if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Get battery status oid="+battery_status_oid);
		String status = snmp.get(battery_status_oid);
		if(status == null){
			// an error occurred
			if(debug) System.out.println("[TrippLite] ["+ip_address+"] Debug: Unable to get the battery condition!!!");
			return battery_good;
		}
		if(status.compareTo(battery_good_str) == 0){
			// the battery is good
			return battery_good;
		} else {
			return battery_bad;
		}
	}
}