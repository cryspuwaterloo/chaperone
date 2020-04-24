package ca.uwaterloo.crysp.chaperone;

import android.app.Service;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
public class RingtoneService extends Service {
    static Ringtone r;

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        //activating alarm sound
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        r = RingtoneManager.getRingtone(getBaseContext(), notification);
        //playing sound alarm
        r.play();

        return START_NOT_STICKY;
    }
    @Override
    public void onDestroy()
    {
        r.stop();
    }
}
