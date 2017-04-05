package limesurvey;

import com.google.gson.JsonArray;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Anael Shomrai
 */
public class TestEmail {

    /**
     * @param jsonLine string received in the response representing json object
     * @return the string resulted from parsing the json
     */
    public static String parse(String jsonLine) {
        JsonElement jelement = new JsonParser().parse(jsonLine);
        JsonObject jobject = jelement.getAsJsonObject();
        String result = jobject.get("result").getAsString();
        return result;
    }

    /**
     * @param json the string received in the response representing json array
     * @return the json array resulted from parsing the string
     */
    public static JsonArray parseArray(String json) {
        JsonElement jelement = new JsonParser().parse(json);
        JsonObject jobject = jelement.getAsJsonObject();
        JsonArray result = jobject.get("result").getAsJsonArray();
        return result;
    }

    /**
     *
     * @param firstName first name of the user
     * @param lastName last name of the user
     * @param email email of the user
     */
    public static void invite_participant(String firstName, String lastName, String email) {
        DefaultHttpClient client;
        HttpPost post;
        HttpResponse response;
        HttpEntity entity;
        final String domain
                = "https://anaelshomrai.limequery.com/index.php/admin/remotecontrol";
        String sessionKey;

        try {
            // Get session key
            client = new DefaultHttpClient();
            post = new HttpPost(domain);
            post.setHeader("Content-type", "application/json");
            post.setEntity(new StringEntity(
                    "{\"method\": \"get_session_key\", \"params\":"
                    + " [\"testEmail\", \"OTqC2cZ2A67O\" ], \"id\": 1}"));
            response = client.execute(post);

            // If Session key received ok continue with the request
            if (response.getStatusLine().getStatusCode() == 200) {
                entity = response.getEntity();
                sessionKey = parse(EntityUtils.toString(entity));

                // Find participent with the params recived (pre-added to the participent list)
                String details = "'email':'" + email + "','firstname':'"
                        + firstName + "','lastname':'" + lastName + "'";

                // list_participants method from the LimeSurvey API
                // params - sessionKey, surveyId,startIdToken, limit,
                // unused token and limit result to the details given
                post.setEntity(new StringEntity(
                        "{\"method\": \"list_participants\", \"params\":"
                        + " [ \"" + sessionKey + "\", \"12345\" ,"
                        + "\"0\" ,\"1\", \"false\","
                        + " \"" + details + "\" ], \"id\": 1}"));
                response = client.execute(post);

                // If response is ok, I excpect to recive 1 result
                // Parsing the result to array and getting the token field
                // Using the token field in the method remind_participants to send email
                if (response.getStatusLine().getStatusCode() == 200) {
                    entity = response.getEntity();
                    JsonArray jsonArray = parseArray(EntityUtils.toString(entity));
                    String token = jsonArray.get(0).getAsJsonObject().get("token").toString();
                    token = token.replace("\"", "");
                    post.setEntity(new StringEntity("{\"method\": \"remind_participants\", \"params\": [ \"" + sessionKey + "\", \"12345\", \"[{"+ token +"}]\" ], \"id\": 1}"));
                    System.out.println(EntityUtils.toString(post.getEntity()));
                    response = client.execute(post);
                    if (response.getStatusLine().getStatusCode() == 200) {
                        entity = response.getEntity();
                        String ans = EntityUtils.toString(entity);
                        System.out.println(ans);

                        // Release session key
                        post.setEntity(new StringEntity(
                                "{\"method\": \"release_session_key\", \"params\":"
                                + " [\"testEmail\", \"OTqC2cZ2A67O\" ], \"id\": 1}"));
                        response = client.execute(post);
                    }
                }
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(TestEmail.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TestEmail.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {
        invite_participant("Anael", "Shomrai", "anaelshomrai@gmail.com");
    }
}
