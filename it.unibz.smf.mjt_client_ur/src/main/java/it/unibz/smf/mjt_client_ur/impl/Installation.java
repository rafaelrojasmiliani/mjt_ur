package it.unibz.smf.mjt_client_ur.impl;

import com.ur.urcap.api.contribution.InstallationNodeContribution;
import com.ur.urcap.api.contribution.InstallationNodeService;

import com.ur.urcap.api.domain.data.DataModel;
import com.ur.urcap.api.domain.URCapAPI;
import com.ur.urcap.api.domain.script.ScriptWriter;

import com.ur.urcap.api.ui.annotation.Img;
import com.ur.urcap.api.ui.annotation.Input;
import com.ur.urcap.api.ui.component.ImgComponent;
import com.ur.urcap.api.ui.component.InputButton;
import com.ur.urcap.api.ui.component.InputCheckBox;
import com.ur.urcap.api.ui.component.InputEvent;
import com.ur.urcap.api.ui.component.InputTextField;

import java.awt.image.BufferedImage;
import java.io.InputStream;

public class Installation implements InstallationNodeContribution {
	// API required for definition of an installation node and screen of
	// a URCap
	public static class Service implements InstallationNodeService {
		public Service() {
		}

		// creates a new installation node instance. the returned node
		// must use the supplied data model object to retrieve and store
		// the data contained in it. the data model object is shared
		// between all installation nodes contributed by the same URCap
		//
		// parameters:
		//      _api - object with access to PolyScope functionality
		//    _model - object where all configuration data of the new
		//             installation node instance is to be stored in and
		//             retrieved from
		// reference:
		// https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/installationnodeservice.html#createInstallationNode-com.ur.urcap.api.domain.URCapAPI-com.ur.urcap.api.domain.data.DataModel-
		@Override
		public Installation createInstallationNode(URCapAPI _api, DataModel _model) {
			return new Installation(_model);
		}

		// text displayed for this URCap in the Installation Tab
		// >> https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/installationnodeservice.html#getTitle--
		@Override
		public String getTitle() {
			return "MJT Planner";
		}

		// return an input stream with the HTML contents of the node
		// >> https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/installationnodeservice.html#getHTML--
		@Override
		public InputStream getHTML() {
			InputStream is = this.getClass().getResourceAsStream("/installation.html");
			return is;
		}
	}

	// DataModel is an interface that provides methods for adding,
	// removing, retrieving, and changing values in a dictionary. Seems
	// that the scope of the model variable is to share data between
	// different installation instances
	// >> https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/domain/data/datamodel.html
	private DataModel model;

	// **interactive HTML element** containing the server IP
	@Input(id = Common.SERVER_IP)
	private InputTextField serverIP;

	// **interactive HTML element callback** to update the server IP
	@Input(id = Common.SERVER_IP)
	private void onServerIPChange(InputEvent _event) {
		Common.setModelValue(model, _event, Common.SERVER_IP, serverIP);
	}

	// **interactive HTML element** containing the server port number
	@Input(id = Common.SERVER_PORT)
	private InputTextField serverPort;

	// **interactive HTML element callback** to update the server port number
	@Input(id = Common.SERVER_PORT)
	private void onServerPortChange(InputEvent _event) {
		Common.setModelValue(model, _event, Common.SERVER_PORT, serverPort);
	}

	// **interactive HTML element** containing the controller's position gain
	@Input(id = Common.CONTROL_KP)
	private InputTextField controlKp;

	// **interactive HTML element callback** to update the controller's position gain
	@Input(id = Common.CONTROL_KP)
	private void onPositionGainChange(InputEvent _event) {
		Common.setModelValue(model, _event, Common.CONTROL_KP, controlKp);
	}

	// **interactive HTML element** containing the controller's velocity gain
	@Input(id = Common.CONTROL_KV)
	private InputTextField controlKv;

	// **interactive HTML element callback** to update the controller's velocity gain
	@Input(id = Common.CONTROL_KV)
	private void onVelocityGainChange(InputEvent _event) {
		Common.setModelValue(model, _event, Common.CONTROL_KV, controlKv);
	}

	// **interactive HTML element** containing the controller's velocity gain
	@Input(id = Common.CONTROL_KA)
	private InputTextField controlKa;

	// **interactive HTML element callback** to update the controller's velocity gain
	@Input(id = Common.CONTROL_KA)
	private void onAccelerationGainChange(InputEvent _event) {
		Common.setModelValue(model, _event, Common.CONTROL_KA, controlKa);
	}

	// **interactive HTML element** containing the sampling time
	@Input(id = Common.SAMPLING_TIME)
	private InputTextField samplingTime;

	// **interactive HTML element callback** to update the sampling time
	@Input(id = Common.SAMPLING_TIME)
	private void onVSamplingTimeChange(InputEvent _event) {
		Common.setModelValue(model, _event, Common.SAMPLING_TIME, samplingTime);
	}

	// **interactive HTML element** to verify the user's consent
	@Input(id = Common.CONSENT)
	InputCheckBox userConsent;

	// **interactive HTML element** to handle the user consent
	@Input(id = Common.CONSENT)
	private void onConsentChange(InputEvent _event) {
		if (_event.getEventType() == InputEvent.EventType.ON_CHANGE) {
			if (userConsent.isSelected()) {
				// activate/deactivate interactive buttons
				enable.setEnabled(true);
				disable.setEnabled(false);

				// set user consent
				model.set(Common.CONSENT, true);
			} else {
				// activate/deactivate interactive buttons
				enable.setEnabled(false);
				disable.setEnabled(false);

				// clear user consent
				model.set(Common.CONSENT, false);
			}

			// clear client state
			model.set(Common.ENABLE, false);
		}
	}

	// **interactive HTML element** to activate the client
	@Input(id = Common.ENABLE)
	InputButton enable;

	// **interactive HTML element callback** to handle client activation
	@Input(id = Common.ENABLE)
	private void onEnablePressed(InputEvent _event) {
		if (_event.getEventType() == InputEvent.EventType.ON_PRESSED) {
			// activate/deactivate interactive buttons
			enable.setEnabled(false);
			disable.setEnabled(true);

			// set client state
			model.set(Common.ENABLE, true);
		}
	}

	// **interactive HTML element** to deactivate the client
	@Input(id = Common.DISABLE)
	InputButton disable;

	// **interactive HTML element callback** to handle client deactivation
	@Input(id = Common.DISABLE)
	private void onDisablePressed(InputEvent _event) {
		if (_event.getEventType() == InputEvent.EventType.ON_PRESSED) {
			// activate/deactivate interactive buttons
			enable.setEnabled(true);
			disable.setEnabled(false);

			// clear client state
			model.set(Common.ENABLE, false);
		}
	}

	// **interactive HTML element** to show the university logo
	@Img(id = Common.UNIBZ)
	ImgComponent unibzLogo;

	// **interactive HTML element** to show the laboratory logo
	@Img(id = Common.SMF)
	ImgComponent smfLogo;

	// class constructor, here we obtain the dictionary model
	// (see the above Server.createInstallationNode() method definition)
	public Installation(DataModel _model) {
		this.model = _model;
	}

    // called each time the user opens this URCap contribution in the Installation Tab
    // >> https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/installationnodecontribution.html#openView--
    @Override
	public void openView() {
		// default client configuration
		serverIP.setText(Common.getModelValue(model, Common.SERVER_IP));
		model.set(Common.SERVER_IP, serverIP.getText());

		serverPort.setText(Common.getModelValue(model, Common.SERVER_PORT));
		model.set(Common.SERVER_PORT, serverPort.getText());

		controlKp.setText(Common.getModelValue(model, Common.CONTROL_KP));
		model.set(Common.CONTROL_KP, controlKp.getText());

		controlKv.setText(Common.getModelValue(model, Common.CONTROL_KV));
		model.set(Common.CONTROL_KV, controlKv.getText());

		controlKa.setText(Common.getModelValue(model, Common.CONTROL_KA));
		model.set(Common.CONTROL_KA, controlKa.getText());

		samplingTime.setText(Common.getModelValue(model, Common.SAMPLING_TIME));
		model.set(Common.SAMPLING_TIME, samplingTime.getText());

		// set/clear user consent and client state
		userConsent.setSelected(model.get(Common.CONSENT, false));
		enable.setEnabled(userConsent.isSelected() && !model.get(Common.ENABLE, false));
		disable.setEnabled(userConsent.isSelected() && model.get(Common.ENABLE, false));

		// university logo
		BufferedImage unibzImg = Common.loadImage(this.getClass().getResourceAsStream("/media/unibz.png"));
		if (null != unibzImg) {
			unibzLogo.setImage(unibzImg);
		}

		// laboratory logo
		BufferedImage smfImg = Common.loadImage(this.getClass().getResourceAsStream("/media/smf.png"));
		if (null != smfImg) {
			smfLogo.setImage(smfImg);
		}
	}

	// called each time the user exits this URCap contribution in the Installation Tab
	// >> https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/installationnodecontribution.html#closeView--
	@Override
	public void closeView() { }

	// defines script code that is added to the beginning of the script code executed when a program is launched
	// >> https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/contribution/installationnodecontribution.html#generateScript-com.ur.urcap.api.domain.script.ScriptWriter-
	@Override
	public void generateScript(ScriptWriter writer) {
		// interface ScriptWriter adds support for generating URScript code from URCaps
		// >> https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/domain/script/scriptwriter.html
	}

	// **custom method** to share data with the program nodes
	public String getValue(String _key) {
		return Common.getModelValue(model, _key);
	}

	// **custom method** to force the user to activate and verify that
	// the data provided in the Installation Tab is complete. program
	// nodes will not get activated until this conditions are fulfilled
	public boolean isDefined() {
		return model.get(Common.CONSENT, false) && model.get(Common.ENABLE, false);
	}
}
