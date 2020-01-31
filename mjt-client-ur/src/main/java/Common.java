package it.unibz.smf.mjt_client_ur.impl;

import com.ur.urcap.api.domain.data.DataModel;

import com.ur.urcap.api.ui.component.ImgComponent;
import com.ur.urcap.api.ui.component.InputButton;
import com.ur.urcap.api.ui.component.InputCheckBox;
import com.ur.urcap.api.ui.component.InputEvent;
import com.ur.urcap.api.ui.component.InputTextField;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import javax.imageio.ImageIO;

public class Common {
	// IDs of common interactive HTML elements
	// (university and laboratory logos)
	public static final String UNIBZ = "UNIBZ";
	public static final String SMF = "SMF";

	// IDs of interactive installation HTML elements
	// (client configuration: server info and controller gains)
	public static final String SERVER_IP = "SERVER_IP";
	public static final String SERVER_PORT = "SERVER_PORT";
	public static final String CONTROL_KP = "CONTROL_KP";
	public static final String CONTROL_KV = "CONTROL_KV";
	public static final String CONTROL_KA = "CONTROL_KA";
	public static final String SAMPLING_TIME = "SAMPLING_TIME";

	// IDs of interactive installation HTML elements
	// (user consent and client activation/deactivation)
	public static final String CONSENT = "CONSENT";
	public static final String ENABLE = "ENABLE";
	public static final String DISABLE = "DISABLE";

	// IDs of interactive program HTML elements
	// (program actions)
	public static final String ADD_WAYPOINT = "ADD_WAYPOINT";
	public static final String GENERATE_PLAN = "GENERATE_PLAN";
	public static final String RESET_PLAN = "RESET_PLAN";

	// IDs of interactive program HTML elements
	// (program input/output interfaces)
	public static final String PROGRAM_NAME = "PROGRAM_NAME";
	public static final String PROGRAM_DISABLED = "PROGRAM_DISABLED";

	// IDs of planning samples (dictionary elements)
	public static final int MIN_WAYPOINTS = 3;
	public static final String WAYPOINTS = "WAYPOINTS";
	public static final String TEMPLATE = "WP%03d";
	public static final String TRAJECTORY = "TRAJECTORY";
	public static final String PATH = "PATH";

	// default values of installation and program elements
	public static final Map<String, String> VALUES;
    static {
        Map<String, String> values = new HashMap<String, String>();
		values.put(SERVER_IP, "10.10.238.1");
		values.put(SERVER_PORT, "5000");
		values.put(CONTROL_KP, "0.0");
		values.put(CONTROL_KV, "0.0");
		values.put(CONTROL_KA, "10.0");
		values.put(SAMPLING_TIME, "0.008");
		values.put(PROGRAM_NAME, "Trajectory");
		VALUES = Collections.unmodifiableMap(values);
    }

	// load images from a given path
	public static BufferedImage loadImage(InputStream _imgStream) {
		BufferedInputStream bufferedInputStream = null;
		BufferedImage bufferedImage = null;
		try {
			bufferedInputStream = new BufferedInputStream(_imgStream);
			bufferedImage = ImageIO.read(bufferedInputStream);
		} catch (IOException e) {
			System.err.println("[mjt-client-ur] unable to load image");
		} finally {
			if (null != bufferedImage) {
				try {
					if(null != bufferedInputStream) {
						bufferedInputStream.close();
					}
				} catch (IOException e) {
					System.err.println("[mjt-client-ur] failed to close image input stream " + e.getMessage());
				}
			}
		}
		return bufferedImage;
	}

	// wrapper function to retrieve data from the model dictionary,
	// providing a default value when the entry is not defined
	public static String getModelValue(DataModel _model, String _key) {
		return _model.get(_key, VALUES.get(_key));
	}

	// wrapper funtion to insert/update entries inside the given model
	// dictionary based on the HTML input element text
	public static void setModelValue(DataModel _model, InputEvent _event, String _key, InputTextField _input) {
		if (_event.getEventType() == InputEvent.EventType.ON_CHANGE) {
			if ("".equals(_input.getText())) {
				_model.set(_key, VALUES.get(_key));
			} else {
				_model.set(_key, _input.getText());
			}
		}
	}
}
