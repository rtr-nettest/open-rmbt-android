package at.rtr.rmbt.client;

import android.os.AsyncTask;

import java.net.HttpURLConnection;
import java.net.URL;

public class AsyncHtmlStatusCodeRetriever extends AsyncTask<String, Void, Integer> {

    private ContentRetrieverListener listener;

    /**
     *
     * @author lb
     *
     */
    public static interface ContentRetrieverListener {
        public void onContentFinished(Integer statusCode);
    }

    /**
     *
     * @param listener
     */
    public void setContentRetrieverListener(ContentRetrieverListener listener) {
        this.listener = listener;
    }

    @Override
    protected Integer doInBackground(String... params) {
        try
        {
            URL url = new URL(params[0]);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                connection.setConnectTimeout(3000);
                connection.connect();
                final int statusCode = connection.getResponseCode();
                System.out.println("response code: " + statusCode);
                return statusCode;

            }
            catch (Exception e) {
                return null;
            }
            finally {
                connection.disconnect();
            }
        }
        catch (Exception e)
        {
            return null;
        }

    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);

        if (this.listener != null) {
            listener.onContentFinished(result);
        }
    }
}
