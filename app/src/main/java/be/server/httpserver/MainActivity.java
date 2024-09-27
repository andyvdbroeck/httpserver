package be.server.httpserver;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpServer;

public class MainActivity extends AppCompatActivity {

    private int port = 8080;
    private String path = "/storage/emulated/0/Download/www";
    private HttpServer server;

    private final Map<String,byte[]> files = new HashMap<>();

    private byte[] readContentIntoByteArray(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] bFile = new byte[(int) file.length()];
        int len = fileInputStream.read(bFile);
        fileInputStream.close();
        if (len != file.length()) {
            Log.v("MainActivity", "File(" + file.getPath() + ")was not read completely.");
        }
        return bFile;
    }

    private String getIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface:interfaces) {
                List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress address : addresses) {
                    if (!address.isLoopbackAddress()) {
                        String ip = address.getHostAddress();
                        if(ip.indexOf(':')<0) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.v("MainActivity", "Exception: " + e.getMessage());
        }
        return "";
    }

    private void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", t -> {
                String file = t.getRequestURI().getPath();
                if (file.isEmpty() || file.equals("/")) file = "/index.html";
                byte[] fileContent = files.get(file);
                if (fileContent == null) {
                    //fileContent = Files.readAllBytes(new File(path+file).toPath());
                    fileContent = readContentIntoByteArray(new File(path + file));
                    files.put(file, fileContent);
                }
                t.sendResponseHeaders(200, fileContent.length);
                OutputStream os = t.getResponseBody();
                os.write(fileContent);
                os.close();
            });
            server.setExecutor(null); // default
            server.start();
        } catch(Exception ioe) {
            Log.v("MainActivity","Exception: " + ioe.getMessage());
        }
    }

    private void stop() {
        if(server!=null) {
            server.stop(0);
        }
        files.clear();
        MainActivity.this.finish();
        System.exit(0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        path = getString(R.string.web_content);
        final String ipAddress = getIpAddress();

        final EditText text = findViewById(R.id.editTextWebContent);
        text.setOnFocusChangeListener((v, hasFocus) -> path = text.getText().toString());
        text.setText(path);

        final ImageButton searchButton = findViewById(R.id.imageButton);
        searchButton.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            startActivityForResult(Intent.createChooser(i, "Choose directory"), 9999);
        });

        final TextView ipAddressText = findViewById(R.id.textViewIpAddress);
        ipAddressText.setText(getString(R.string.ipaddress,getIpAddress(),port));

        final EditText portText = findViewById(R.id.editTextPort);
        portText.setText(getString(R.string.port,port));
        portText.setOnFocusChangeListener((v, hasFocus) -> {
            try {
                port = Integer.parseInt(portText.getText().toString());
            } catch(NumberFormatException nfe) {
                port = 80;
            }
            portText.setText(getString(R.string.port,port));
            ipAddressText.setText(getString(R.string.ipaddress,ipAddress,port));
        });

        final ImageButton buttonUp = findViewById(R.id.imageButtonUp);
        buttonUp.setOnClickListener(v -> {
            port++;
            portText.setText(getString(R.string.port,port));
            ipAddressText.setText(getString(R.string.ipaddress,ipAddress,port));
        });

        final ImageButton buttonDown = findViewById(R.id.imageButtonDown);
        buttonDown.setOnClickListener(v -> {
            port--;
            portText.setText(getString(R.string.port,port));
            ipAddressText.setText(getString(R.string.ipaddress,ipAddress,port));
        });

        final Button button = findViewById(R.id.button);
        button.setOnClickListener(v -> {
            if(button.getText().equals(getString(R.string.start))) {
                start();
                button.setText(R.string.stop);
            } else {
                stop();
                button.setText(R.string.start);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode == 9999) {
                String p = data.getData().getPath();
                path = p.substring(p.indexOf(":")+1);
                EditText text = findViewById(R.id.editTextWebContent);
                text.setText(path);
        }
    }
}