package linsipjunior.rootio.org.linsipjunior;

import android.content.ContentValues;

import org.linphone.core.Call;
import org.linphone.core.RegistrationState;

/**
 * Created by Jude Mukundane on 3/9/2018.
 */

public interface SipEventsNotifiable {
    void updateCallState(Call.State callState, Call call, ContentValues values);

    void updateRegistrationState(RegistrationState registrationState, ContentValues values);
}
