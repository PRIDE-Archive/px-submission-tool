package uk.ac.ebi.pride.gui.task;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.util.Constant;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.*;

/**
 * Abstract class provides methods for communicating with Pride Web Service
 *
 * @author Rui Wang
 * @version $Id$
 */
public abstract class AbstractWebServiceTask<T> extends TaskAdapter<T, String> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractWebServiceTask.class);

    /**
     * Build the url for retrieving from pride web service
     */
    public String buildWebServiceURL(String baseUrl, Map<String, String> arguments) {
        StringBuilder cmd = new StringBuilder();

        try {
            cmd.append(baseUrl);
            if (arguments != null && arguments.size() > 0) {
                cmd.append("?");
                for (Map.Entry<String, String> argument : arguments.entrySet()) {
                    if (cmd.length() != 0) {
                        cmd.append("&");
                    }
                    cmd.append(argument.getKey());
                    cmd.append("=");
                    cmd.append(URLEncoder.encode(argument.getValue(), "UTF-8"));
                }
            }
        } catch (IOException ex) {
            logger.warn("Fail to send construct url for PRIDE web service: {}", ex.getMessage());
        }

        return cmd.toString();
    }

    /**
     * Download user details
     *
     * @param url http url
     * @return Map<String, String>   pride user details
     */
    public Map<String, String> queryWebServiceForSingleEntry(String url) {
        Map<String, String> result = null;
        BufferedReader in = null;

        try {
            logger.debug("Loading pride web service output ...");
            HttpResponse httpResponse = doHttpGet(url);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            HttpEntity httpEntity = httpResponse.getEntity();
            if (statusCode == 200 && httpEntity != null) {
                in = new BufferedReader(new InputStreamReader(httpEntity.getContent()));
                result = new LinkedHashMap<String, String>();
                String str;
                while ((str = in.readLine()) != null) {
                    if (!"".equals(str)) {
                        logger.debug(str);
                        String[] parts = str.split(Constant.TAB);
                        result.put(parts[0], parts[1]);
                    }
                }
                in.close();
            } else {
                if (statusCode == 404) {
                    logger.warn("Fail to connect to remote PRIDE server: {}", httpResponse.getStatusLine().getReasonPhrase());
                    publish("Fail to connect to PRIDE, please ensure you have Internet connection");
                } else {
                    logger.warn("Wrong login credentials: {}", httpResponse.getStatusLine().getReasonPhrase());
                    publish("Failed to login, please check user name or password");
                }
            }
        } catch (IOException ex) {
            String msg = ex.getMessage();
            if (msg.contains("403")) {
                logger.warn("Wrong login credentials: {}", msg);
                publish("Failed to login, please check user name or password");
            } else {
                logger.warn("Fail to connect to remote PRIDE server: {}", msg);
                publish("Fail to connect to PRIDE, please ensure you have Internet connection");
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.warn("Failed to close output stream", e);
                }
            }
        }

        return result;
    }

    public List<Map<String, String>> queryWebServiceForMultipleEntries(String url) {
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        BufferedReader in = null;

        try {
            logger.debug("Loading pride web service output ...");
            HttpResponse httpResponse = doHttpGet(url);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            HttpEntity httpEntity = httpResponse.getEntity();
            if (statusCode == 200 && httpEntity != null) {
                in = new BufferedReader(new InputStreamReader(httpEntity.getContent()));
                Map<String, String> result = new HashMap<String, String>();
                String str;
                while ((str = in.readLine()) != null) {
                    str = str.trim();
                    if (!"".equals(str)) {
                        if ("//".equals(str)) {
                            results.add(result);
                            result = new HashMap<String, String>();
                        } else {
                            logger.debug(str);
                            String[] parts = str.split(Constant.TAB);
                            result.put(parts[0], parts[1]);
                        }
                    }
                }
                in.close();
            } else {
                if (statusCode == 404) {
                    logger.warn("Fail to connect to remote PRIDE server: {}", httpResponse.getStatusLine().getReasonPhrase());
                    publish("Fail to connect to PRIDE, please ensure you have Internet connection");
                } else {
                    logger.warn("Wrong login credentials: {}", httpResponse.getStatusLine().getReasonPhrase());
                    publish("Failed to login, please check user name or password");
                }
            }
        } catch (IOException ex) {
            String msg = ex.getMessage();
            if (msg.contains("403")) {
                logger.warn("Wrong login credentials: {}", msg);
                publish("Failed to login, please check user name or password");
            } else {
                logger.warn("Fail to connect to remote PRIDE server: {}", msg);
                publish("Fail to connect to PRIDE, please ensure you have Internet connection");
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.warn("Failed to close output stream", e);
                }
            }
        }

        return results;
    }

    /**
     * Perform a http get request using a given url
     */
    private HttpResponse doHttpGet(String url) throws IOException {
        HttpClient httpClient = new DefaultHttpClient();

        // set proxy
        Properties props = System.getProperties();
        String proxyHost = props.getProperty("http.proxyHost");
        String proxyPort = props.getProperty("http.proxyPort");
        if (proxyHost != null && proxyPort != null) {
            HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort));
            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }

        HttpGet httpGet = new HttpGet(url);
        return httpClient.execute(httpGet);
    }
}
