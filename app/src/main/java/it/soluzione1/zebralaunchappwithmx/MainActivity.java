package it.soluzione1.zebralaunchappwithmx;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {


    Sdk _sdk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        final Context context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button button = findViewById(R.id.button_launch_app);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {

                    if(_sdk == null)
                        throw new Exception(getString(R.string.sdk_not_initialized));

                    if(!_sdk.isSdkReady())
                        throw new Exception(getString(R.string.sdk_not_ready));

                    String appName =((EditText)findViewById(R.id.edit_text)).getText().toString();

                    if( !appName.trim().equals("")){
                        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><wap-provisioningdoc>\n" +
                                "  <characteristic type=\"Profile\">\n" +
                                "    <parm name=\"ProfileName\" value=\"ExecuteExtension\"/>\n" +
                                "    <parm name=\"ModifiedDate\" value=\"2020-03-14 18:46:07\"/>\n" +
                                "    <parm name=\"TargetSystemVersion\" value=\"6.0\"/>\n" +
                                "    <characteristic type=\"AppMgr\" version=\"5.1\">\n" +
                                "      <parm name=\"emdk_name\" value=\"LaunchApplication\"/>\n" +
                                "      <parm name=\"Action\" value=\"LaunchApplication\"/>\n" +
                                "      <parm name=\"ApplicationName\" value=\"CHANGEME\"/>\n" +
                                "    </characteristic>\n" +
                                "  </characteristic>\n" +
                                "</wap-provisioningdoc>";

                        xml = xml.replace("CHANGEME", appName);
                       _sdk.executeCommand( xml, "ExecuteCommand" );
                    }
                }
                catch (Exception ex){

                    Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();

                }

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(_sdk == null && isZebraDevice())
            _sdk = new ZebraEMDKApiWrapper();

    }

    @Override
    protected void onPause() {
        super.onPause();

        if(_sdk != null)
            _sdk.release();

    }

    public  static  boolean isZebraDevice(){
        return (android.os.Build.MANUFACTURER.contains("Zebra Technologies") || android.os.Build.MANUFACTURER.contains("Motorola Solutions") );
    }
}
