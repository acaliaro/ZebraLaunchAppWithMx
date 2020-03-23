package it.soluzione1.zebralaunchappwithmx;

import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import com.symbol.emdk.EMDKBase;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;
import com.symbol.emdk.VersionManager;
import com.symbol.emdk.personalshopper.PersonalShopper;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class ZebraEMDKApiWrapper extends Sdk implements EMDKManager.EMDKListener, EMDKManager.StatusListener/*, Scanner.StatusListener*/ {

    private static final String PROFILE_EXECUTE_EXTENSION = "ExecuteExtension";

    private static final String TAG = ZebraEMDKApiWrapper.class.getName();

    private PersonalShopper _personalShopper = null;
    private EMDKManager _emdkManager = null;
    private ProfileManager _profileManager = null;
    private String _barcodeVersion;
    private String _emdkVersion;
    private String _mxVersion;
    private boolean _fromQueue;
    private List<FeatureTypeDto> _featureTypes;

    public ZebraEMDKApiWrapper() {

        // The EMDKManager object will be created and returned in the callback.
        EMDKResults results = EMDKManager.getEMDKManager(MyCustomApplication.getAppContext(), this);
        // Check the return status of getEMDKManager and update the status Text
        // View accordingly
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS)
            Toast.makeText(MyCustomApplication.getAppContext(), MyCustomApplication.getAppContext().getString(R.string.emdkmanager_request_failed), Toast.LENGTH_LONG).show();

        _featureTypes = new ArrayList<>();
        _featureTypes.add(new FeatureTypeDto(EMDKManager.FEATURE_TYPE.PROFILE, false));
//        _featureTypes.add(new FeatureTypeDto(EMDKManager.FEATURE_TYPE.VERSION, false));
//        _featureTypes.add(new FeatureTypeDto(EMDKManager.FEATURE_TYPE.PERSONALSHOPPER, false));

    }

    @Getter
    @Setter
    @AllArgsConstructor
    private static class FeatureTypeDto {
        private EMDKManager.FEATURE_TYPE featureType;
        private boolean enabled;
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {

        _emdkManager = emdkManager;

        try {

            for (FeatureTypeDto featureType: _featureTypes) {
                emdkManager.getInstanceAsync(featureType.getFeatureType(), this);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onStatus(EMDKManager.StatusData statusData, EMDKBase emdkBase) {

        boolean error = false;

        Log.d(TAG, "onStatus");
        try {

            // Considero sia SUCCESS che FEATURE_NOT_SUPPORTED in quanto su device non aventi PersonalShopper posso comunque utilizzare Smartilio.
            // Quindi il device che riceverà FEATURE_NOT_SUPPORTED avrà "enabled" a true ma non gestirà le funzionalità di PersonalShopper. Valutare poi
            // se modificare la classe FeatureType per distinguere tra SUCCESS e FEATURE_NOT_SUPPORTED
            if (statusData.getResult() == EMDKResults.STATUS_CODE.SUCCESS ||
                    statusData.getResult() == EMDKResults.STATUS_CODE.FEATURE_NOT_SUPPORTED) {

                Log.d(TAG, "onStatus success = true");

                if (statusData.getFeatureType() == EMDKManager.FEATURE_TYPE.PROFILE && (emdkBase == null || emdkBase.getType() == EMDKManager.FEATURE_TYPE.PROFILE)) {

                    Log.d(TAG, "onStatus PROFILE");

                    setFeatureType(EMDKManager.FEATURE_TYPE.PROFILE, true);

//<<<<<<< HEAD
                    if (statusData.getResult() == EMDKResults.STATUS_CODE.SUCCESS) {
                        _profileManager = (ProfileManager) emdkBase;
                        if (_profileManager != null) {
                            _profileManager.addDataListener(new ProfileManager.DataListener() {
                                @Override
                                public void onData(ProfileManager.ResultData resultData) {

                                    EMDKResults results = resultData.getResult();

                                    //Check the return status of processProfile
                                    if (results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
                                    } else if (results.statusCode == EMDKResults.STATUS_CODE.CHECK_XML) {
                                        ZebraEMDKApiWrapper.this.parseXML(_fromQueue, results.getStatusString());
                                    } else {
                                    }
                                }
                            });
                        }
                    }
                } else {
                    Log.d(TAG, "onStatus UNKNOWN");
                    error = true;
                }
            } else {
                Log.d(TAG, "onStatus success = false");

                error = true;
            }

            if (error) {
                Log.d(TAG, "onStatus errore");

            } else {

                // se tutti i profili sono ok, invio messaggio alla activity
                if (featureTypeEnabled()) {
                    Log.d(TAG, "onStatus featureTypeEnabled");
                }
            }
        }
        catch (Exception e){

            e.printStackTrace();
        }
    }

    private void setFeatureType(EMDKManager.FEATURE_TYPE featureType, boolean enabled) {

        for(int i = 0; i <= _featureTypes.size(); i++){
            if(_featureTypes.get(i).getFeatureType().equals(featureType)){
                _featureTypes.get(i).setEnabled(enabled);
                break;
            }
        }
    }

    private boolean featureTypeEnabled(){

        for(int i = 0; i < _featureTypes.size(); i++){
            if(!_featureTypes.get(i).isEnabled()){
                return  false;
            }
        }

        return  true;
    }

    @Override
    public String BarcodeVersion() {
        return _barcodeVersion;
    }

    @Override
    public boolean isSdkReady() {
        return featureTypeEnabled();
    }

    @Override
    public String SdkVersion() {
        return _emdkVersion;
    }

    @Override
    public String ExtensionVersion() {
        return _mxVersion;
    }

    @Override
    public void onClosed() {

        releaseEmdk();
    }

    private boolean isPersonalShopperValid(){
        return  _personalShopper != null && _personalShopper.cradle != null;
    }

    @Override
    public void release() {

        _personalShopper = null;

        if (_profileManager != null)
            _profileManager = null;

        releaseEmdk();
    }

    @Override
    public void executeCommand( String xml, String command) {
        mxExecuteTask(  xml, command);
    }

    private void  releaseEmdk() {

        // Clean up the objects created by EMDK manager
        if (_emdkManager != null) {
            _emdkManager.release();
            _emdkManager = null;
        }
    }

    private void mxExecuteTask( String xml, String command){

        String profile = null;

        try {

            profile = command;

            String[] param = new String[1];
            param[0] = null;

            //Call process profile to modify the profile of specified profile name
            if(_profileManager != null){
                EMDKResults results;

                // Se ho ricevuto un xml, lo eseguo, altrimenti esegui direttamente il profilo che ho nel EMDKConfig
                if(xml == null)
                    results = _profileManager.processProfileAsync(profile, ProfileManager.PROFILE_FLAG.SET, param);
                else{

                    // Prima verifico se la stringa ricevuta è effettivamente un xml
                    if(!isXmlString(xml))
                        throw new Exception(MyCustomApplication.getAppContext().getString(R.string.stringa_non_xml));

                    // Ricordarsi che i caratteri tipo \n nell'xml danno problemi occorre toglierli
                    results = _profileManager.processProfileAsync(PROFILE_EXECUTE_EXTENSION, ProfileManager.PROFILE_FLAG.SET, new String[]{xml.replace("\n", "").replace("\r","").replace("\t", "")});
                }

            }
        }
        catch (Exception e){


            Log.e(TAG,  profile + " " + Objects.requireNonNull(e.getMessage()));


        }
    }

    private boolean isXmlString(String xml) {

        boolean ok = true;
        try {
            // La stringa non deve essere vuota
            if(xml != null) {

                // La stringa deve contenere wap-provisioningdoc
                if(!xml.contains("wap-provisioningdoc"))
                    throw new Exception();

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();

                xpp.setInput(new StringReader(xml)); // pass input whatever xml you have
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_DOCUMENT) {
                        Log.d(TAG, "Start document");
                    } else if (eventType == XmlPullParser.START_TAG) {
                        Log.d(TAG, "Start tag " + xpp.getName());
                    } else if (eventType == XmlPullParser.END_TAG) {
                        Log.d(TAG, "End tag " + xpp.getName());
                    } else if (eventType == XmlPullParser.TEXT) {
                        Log.d(TAG, "Text " + xpp.getText()); // here you get the text from xml
                    }

                    eventType = xpp.next();

                }
            }
            else
                throw new Exception();
        }
        catch (Exception ex){
            ok = false;
        }

        return ok;
    }

    // Method to parse the XML response using XML Pull Parser
    private void parseXML(boolean fromQueue,  String statusXmlResponse) {
        int event;
        try {
            String parmName;
            String errorDescription;
            String errorString = null;
            String errorType;
            boolean hasError = false;

            XmlPullParser parser = Xml.newPullParser();
            // Provide the string response to the String Reader that reads
            // for the parser
            parser.setInput(new StringReader(statusXmlResponse));

            // Retrieve error details if parm-error/characteristic-error in the response XML
            event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();
                switch (event) {
                    case XmlPullParser.START_TAG:

                        if (name.equals("parm-error")) {
                            hasError = true;
                            parmName = parser.getAttributeValue(null, "name");
                            errorDescription = parser.getAttributeValue(null, "desc");
                            errorString = " (Name: " + parmName + ", Error Description: " + errorDescription + ")";

                            Log.d(TAG,"parseXml 1" + errorString);
                            break;
                        }

                        if (name.equals("characteristic-error")) {
                            hasError = true;
                            errorType = parser.getAttributeValue(null, "type");
                            errorDescription = parser.getAttributeValue(null, "desc");
                            errorString = " (Type: " + errorType + ", Error Description: " + errorDescription + ")";

                            Log.d(TAG,"parseXml 2" + errorString);

                            break;
                        }

                        break;
                    case XmlPullParser.END_TAG:

                        break;
                }

                event = parser.next();
            }

            Log.d(TAG, "parseXml " + hasError + " " + errorString);
            // Per il reboot questo valore dovrebbe essere false

            if(hasError)
                Toast.makeText(MyCustomApplication.getAppContext(), errorString, Toast.LENGTH_LONG).show();


        } catch (Exception e) {


            e.printStackTrace();
        }
    }

}
