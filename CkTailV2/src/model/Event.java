package model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;

import main.split.Regex;

/**
 * @author Blot Elliott
 *
 */
public class Event {

	public static String from = "Host";
	public static String to = "Dest";

	private String session;
	
	private String label;
	private String separator = "|||";
	private ArrayList<String> params;
	private ArrayList<String> paramsSess;
	public String date;  //public for debug only


	public Event(String line, Matcher m) {
		date = m.group("date");
		label = m.group("label");
		params = new ArrayList<String>();
		paramsSess = new ArrayList<String>();
		int n = 1;
		try {
			while(m.group("param" + n) != null) {
				if (m.group("param" + n).contains("session=")) {
					//System.out.println(m.group("param" + n));
					session = m.group("param" + n);
				}
				else {
					params.add(m.group("param" + n));
					
				}
				paramsSess.add(m.group("param" + n)); 
				n++;
			}
		}catch(IllegalArgumentException e) {
			//end of while
		}
	}
	
	/**
	 * Return the label of the event.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Return the parameters of the event, without session identifier.
	 */
	public ArrayList<String> getparams() {
		return params;
	}
	
	/**
	 * Return all the parameters.
	 */
	public ArrayList<String> getparamsSess() {
		return paramsSess;
	}
	
	/**
	 * Return the source of the event.
	 */
	public String getFrom() {
		String res = "";
		for (String param:params) {
			if (param.startsWith(from + "=")){
				return param.substring(from.length() + 1);
			}
		}
		System.err.println("no From in :" + this.toString());
		System.exit(3);
		return res;
	}

	/**
	 * Return the destination of the event.
	 */
	public String  getTo() {
		String res = "";
		for (String param:params) {
			if (param.startsWith(to + "=")){
				return param.substring(to.length() + 1);
			}
		}
		System.err.println("no From in :" + this.toString());
		System.exit(3);
		return res;
	}

	/**
	 * Return the timestamps of the event.
	 */
	public Date getDate(Regex regex) {
		SimpleDateFormat sdf = regex.getSDF();
		Date d = null;
		try {
			d = sdf.parse(date);
		} catch (ParseException e) {
			System.err.println("problem with date format");
			System.exit(3);
		}
		return d;
	}

	public String toString() {
		String res = label + "(";
		//res = res + "date=" + date + ";";//for debug only
		for (String param: params) {
			res = res + param + separator;
		}
		res = res.substring(0, res.length()- separator.length());
		res = res +")";
		return res;
	}

	/**
	 * Print the event for debugging purposes.
	 */
	public String debug() {
		String res = label + "(";
		res = res + date;
		res = res + params.get(0);
		res = res + params.get(1);
		res = res +")\n";
		return res;
	}

	/**
	 * Check if ai has common parameters with this.
	 */
	public boolean dataSimilarity(Event ai) {
		ArrayList<String> paramsi = ai.getparamsSess();
		ArrayList<String> paramsj = this.getparamsSess();
		for (String parami: paramsi) {
			if (!(parami.contains(from) || parami.contains(to))) {
				for (String paramj: paramsj) {
					if (paramj.equals(parami)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Check if the event is a request.
	 */
	public boolean isReq() {
		if (!this.toString().contains("esponse") & !this.toString().contains("Resp") & !this.toString().contains("resp") & !isInter()) {
			//System.out.println(this);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Check if the event is a response.
	 */
	public boolean isResp() {
		if (this.toString().contains("esponse") | this.toString().contains("Resp") | this.toString().contains("resp")) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Check if the event is a non-communicating action.
	 */
	public boolean isInter() {
		if (!this.toString().contains("Host=") | !this.toString().contains("Dest=")) {
			return true;
		}
		if (getFrom().equals(getTo())) {
			//System.out.println(this);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Return the session identifier.
	 */
	public String getSessionID() {
		return session;
	}


}

