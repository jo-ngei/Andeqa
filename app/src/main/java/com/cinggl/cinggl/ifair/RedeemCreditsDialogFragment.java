package com.cinggl.cinggl.ifair;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.cinggl.cinggl.Constants;
import com.cinggl.cinggl.R;
import com.cinggl.cinggl.models.Balance;
import com.cinggl.cinggl.models.Cingle;
import com.cinggl.cinggl.models.TransactionDetails;
import com.cinggl.cinggl.profile.UpdateProfileActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;

import static android.R.attr.filter;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;
import static com.cinggl.cinggl.R.id.cingleSenseCreditsTextView;

/**
 * A simple {@link Fragment} subclass.
 */
public class RedeemCreditsDialogFragment extends DialogFragment implements View.OnClickListener {
    @Bind(R.id.amountEnteredEditText)EditText mAmountEnteredEditText;
    @Bind(R.id.redeemAmountButton)Button mRedeemAmountButton;

    private String mPostKey;
    private static final String EXTRA_POST_KEY = "post key";
    private FirebaseAuth firebaseAuth;
    private DatabaseReference cinglesReference;
    private DatabaseReference cingleWalletReference;
    private DatabaseReference walletReference;
    private DatabaseReference transactionReference;
    private boolean redeemCSC = false;

    //REMOVE SCIENTIFIC NOATATION
    private DecimalFormat formatter =  new DecimalFormat("0.00000000");

    public static RedeemCreditsDialogFragment newInstance(String title){
        RedeemCreditsDialogFragment redeemCreditsDialogFragment = new RedeemCreditsDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        redeemCreditsDialogFragment.setArguments(args);
        return  redeemCreditsDialogFragment;

    }

    public RedeemCreditsDialogFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_redeem_credits_dialog, container, false);
        ButterKnife.bind(this, view);

        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() != null){
            cinglesReference = FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_CINGLES);
            walletReference = FirebaseDatabase.getInstance().getReference(Constants.WALLET);
            cingleWalletReference = FirebaseDatabase.getInstance().getReference(Constants.CINGLE_WALLET);
            transactionReference = FirebaseDatabase.getInstance().getReference(Constants.TRANSACTION_HISTORY)
                    .child(firebaseAuth.getCurrentUser().getUid());

            Bundle bundle = getArguments();
            if (bundle != null){
                mPostKey = bundle.getString(RedeemCreditsDialogFragment.EXTRA_POST_KEY);

                Log.d("the passed poskey", mPostKey);

            }else {
                throw new IllegalArgumentException("pass an EXTRA_POST_KEY");
            }

            //initialize input filters
            setEditTextFilter();
            mRedeemAmountButton.setOnClickListener(this);

        }
        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String title = getArguments().getString("title", "redeem credits");
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    public void onClick(View v) {
        if (v == mRedeemAmountButton) {
            if (mAmountEnteredEditText != null) {
                final String amountInString = mAmountEnteredEditText.getText().toString();

                final double amountEntered = Double.parseDouble(amountInString);
                final String formattedString = formatter.format(amountEntered);
                final double amountTransferred = Double.parseDouble(formattedString);

                cinglesReference.child(mPostKey).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                         if (dataSnapshot.exists()){
                             Cingle cingle = dataSnapshot.getValue(Cingle.class);
                             final double sensepoint = cingle.getSensepoint();
                             Log.d("amount of sensepoint", sensepoint + "");

                             if (mAmountEnteredEditText.getText().equals("")) {
                                 mAmountEnteredEditText.setError("Amount cannot be empty");
                             } else if (amountTransferred > sensepoint) {
                                 mAmountEnteredEditText.setError("Your Cingle has insufficient CSC balance");
                             }else if (amountTransferred < 0.00){
                                 mAmountEnteredEditText.setError("Amount cannot be zero");
                             }else {
                                 //RECORD BALANCE AMOUNT AFTER REDEMPTION
                                 final double sensecredits = sensepoint - amountTransferred;

                                 Log.d("amount of sensecredits", sensecredits + "");

                                 cinglesReference.child(mPostKey).child("sensepoint").setValue(sensecredits);

                                 //get the current date
                                 SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d");
                                 String date = simpleDateFormat.format(new Date());

                                 if (date.endsWith("1") && !date.endsWith("11"))
                                     simpleDateFormat = new SimpleDateFormat("d'st' MMM yyyy");
                                 else if (date.endsWith("2") && !date.endsWith("12"))
                                     simpleDateFormat = new SimpleDateFormat("d'nd' MMM yyyy");
                                 else if (date.endsWith("3") && !date.endsWith("13"))
                                     simpleDateFormat = new SimpleDateFormat("d'rd' MMM yyyy");
                                 else
                                     simpleDateFormat = new SimpleDateFormat("d'th' MMM yyyy");
                                 final String currentDate = simpleDateFormat.format(new Date());

                                 //INCREAMENT THE AMOUNT TRANSFERED AFTER NEW TRANSFERS
                                 cingleWalletReference.child(mPostKey).child("amount redeemed")
                                         .addListenerForSingleValueEvent(new ValueEventListener() {
                                             @Override
                                             public void onDataChange(DataSnapshot dataSnapshot) {
                                                     //IF TRANSACTIONS HAVE BEEN DONE BEFORE
                                                     if (dataSnapshot.exists()){
                                                         final Double currentAmount = (Double)dataSnapshot.getValue();
                                                         Log.d("before redeemption", currentAmount + "");
                                                         final double newAmount = currentAmount + amountTransferred;
                                                         Log.d("after redeemed", newAmount + "");
                                                         cingleWalletReference.child(mPostKey).child("amount redeemed")
                                                                 .setValue(newAmount).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                             @Override
                                                             public void onComplete(@NonNull Task<Void> task) {
                                                                 if (task.isSuccessful()){
                                                                     //RECORD THE REDEEMED AMOUNT TRANSFERRED TO THE USE WALLET
                                                                     final Balance balance = new Balance();
                                                                     balance.setTotalBalance(amountTransferred);
                                                                     walletReference.child("balance").child(firebaseAuth.getCurrentUser().getUid())
                                                                             .child("total balance").addListenerForSingleValueEvent(new ValueEventListener() {
                                                                         @Override
                                                                         public void onDataChange(DataSnapshot dataSnapshot) {
                                                                             if (dataSnapshot.exists()){
                                                                                 final Double currentBalance = (Double) dataSnapshot.getValue();
                                                                                 final double newBalance = currentBalance + amountTransferred;
                                                                                 Log.d("new balance", newBalance + "");

                                                                                 //set transaction details
                                                                                 final TransactionDetails transactionDetails = new TransactionDetails();
                                                                                 transactionDetails.setAmount(amountTransferred);
                                                                                 transactionDetails.setUid(firebaseAuth.getCurrentUser().getUid());
                                                                                 transactionDetails.setPostId(mPostKey);
                                                                                 transactionDetails.setDatePosted(currentDate);
                                                                                 transactionDetails.setWalletBalance(newBalance);
                                                                                 //get the push id
                                                                                 DatabaseReference ref = transactionReference.push();
                                                                                 String pushId = ref.getKey();
                                                                                 Log.d("transaction push id", pushId);
                                                                                 //set the push id
                                                                                 transactionDetails.setPushId(pushId);
                                                                                 ref.setValue(transactionDetails);

                                                                                 walletReference.child("balance").child(firebaseAuth.getCurrentUser()
                                                                                         .getUid()).child("total balance").setValue(newBalance)
                                                                                         .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                             @Override
                                                                                             public void onComplete(@NonNull Task<Void> task) {
                                                                                                 if (task.isComplete()){
                                                                                                     Toast.makeText(getContext(), "Transaction successful",
                                                                                                             Toast.LENGTH_LONG).show();
                                                                                                 }else {
                                                                                                     Toast.makeText(getContext(), "Transaction not successful. " +
                                                                                                             "Please try again later",
                                                                                                             Toast.LENGTH_LONG).show();
                                                                                                 }
                                                                                             }
                                                                                         });

                                                                             }else {
                                                                                 //set transaction details
                                                                                 final TransactionDetails transactionDetails = new TransactionDetails();
                                                                                 transactionDetails.setAmount(amountTransferred);
                                                                                 transactionDetails.setUid(firebaseAuth.getCurrentUser().getUid());
                                                                                 transactionDetails.setPostId(mPostKey);
                                                                                 transactionDetails.setDatePosted(currentDate);
                                                                                 transactionDetails.setWalletBalance(amountTransferred);
                                                                                 //get the push id
                                                                                 DatabaseReference ref = transactionReference.push();
                                                                                 String pushId = ref.getKey();
                                                                                 Log.d("transaction push id", pushId);
                                                                                 //set the push id
                                                                                 transactionDetails.setPushId(pushId);
                                                                                 ref.setValue(transactionDetails);

                                                                                 walletReference.child("balance").child(firebaseAuth.getCurrentUser()
                                                                                         .getUid()).child("total balance").setValue(amountTransferred)
                                                                                         .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                             @Override
                                                                                             public void onComplete(@NonNull Task<Void> task) {
                                                                                                 if (task.isComplete()){
                                                                                                     Toast.makeText(getContext(), "Transaction successful. " +
                                                                                                             "Please try again later",
                                                                                                             Toast.LENGTH_LONG).show();
                                                                                                 }
                                                                                             }
                                                                                         });

                                                                             }
                                                                         }

                                                                         @Override
                                                                         public void onCancelled(DatabaseError databaseError) {

                                                                         }
                                                                     });
                                                                 }
                                                             }
                                                         });
                                                     }else {
                                                         //IF THE TRANSACTIONS IS FOR THE FIRST TIME
                                                         cingleWalletReference.child(mPostKey).child("amout redeemed")
                                                                 .setValue(amountTransferred)
                                                                 .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                             @Override
                                                             public void onComplete(@NonNull Task<Void> task) {
                                                                 if (task.isSuccessful()){
                                                                     //RECORD THE REDEEMED AMOUNT TRANSFERRED TO THE USE WALLET
                                                                     final Balance balance = new Balance();
                                                                     balance.setTotalBalance(amountTransferred);
                                                                     walletReference.child("balance").child(firebaseAuth.getCurrentUser().getUid())
                                                                             .child("total balance").addListenerForSingleValueEvent(new ValueEventListener() {
                                                                         @Override
                                                                         public void onDataChange(DataSnapshot dataSnapshot) {
                                                                             if (dataSnapshot.exists()){
                                                                                 final Double currentBalance = (Double) dataSnapshot.getValue();
                                                                                 final double newBalance = currentBalance + amountTransferred;
                                                                                 Log.d("new balance", newBalance + "");

                                                                                 //set transaction details
                                                                                 final TransactionDetails transactionDetails = new TransactionDetails();
                                                                                 transactionDetails.setAmount(amountTransferred);
                                                                                 transactionDetails.setUid(firebaseAuth.getCurrentUser().getUid());
                                                                                 transactionDetails.setPostId(mPostKey);
                                                                                 transactionDetails.setDatePosted(currentDate);
                                                                                 transactionDetails.setWalletBalance(newBalance);
                                                                                 //get the push id
                                                                                 DatabaseReference ref = transactionReference.push();
                                                                                 String pushId = ref.getKey();
                                                                                 Log.d("transaction push id", pushId);
                                                                                 //set the push id
                                                                                 transactionDetails.setPushId(pushId);
                                                                                 ref.setValue(transactionDetails);

                                                                                 walletReference.child("balance").child(firebaseAuth.getCurrentUser()
                                                                                         .getUid()).child("total balance").setValue(newBalance)
                                                                                         .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                             @Override
                                                                                             public void onComplete(@NonNull Task<Void> task) {
                                                                                                 if (task.isComplete()){
                                                                                                     Toast.makeText(getContext(), "Transaction successful",
                                                                                                             Toast.LENGTH_LONG).show();
                                                                                                 }else {
                                                                                                     Toast.makeText(getContext(), "Transaction not successful. Please try again later",
                                                                                                             Toast.LENGTH_LONG).show();
                                                                                                 }
                                                                                             }
                                                                                         });
                                                                             }else {
                                                                                 //set transaction details
                                                                                 final TransactionDetails transactionDetails = new TransactionDetails();
                                                                                 transactionDetails.setAmount(amountTransferred);
                                                                                 transactionDetails.setUid(firebaseAuth.getCurrentUser().getUid());
                                                                                 transactionDetails.setPostId(mPostKey);
                                                                                 transactionDetails.setDatePosted(currentDate);
                                                                                 transactionDetails.setWalletBalance(amountTransferred);
                                                                                 //get the push id
                                                                                 DatabaseReference ref = transactionReference.push();
                                                                                 String pushId = ref.getKey();
                                                                                 Log.d("transaction push id", pushId);
                                                                                 //set the push id
                                                                                 transactionDetails.setPushId(pushId);
                                                                                 ref.setValue(transactionDetails);

                                                                                 walletReference.child("balance").child(firebaseAuth.getCurrentUser()
                                                                                         .getUid()).child("total balance").setValue(amountTransferred)
                                                                                         .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                             @Override
                                                                                             public void onComplete(@NonNull Task<Void> task) {
                                                                                                 if (task.isComplete()){
                                                                                                     Toast.makeText(getContext(), "Transaction not successful. Please try again later",
                                                                                                             Toast.LENGTH_LONG).show();
                                                                                                 }
                                                                                             }
                                                                                         });
                                                                             }
                                                                         }

                                                                         @Override
                                                                         public void onCancelled(DatabaseError databaseError) {

                                                                         }
                                                                     });
                                                                 }
                                                             }
                                                         });
                                                     }

                                             }

                                             @Override
                                             public void onCancelled(DatabaseError databaseError) {

                                             }
                                         });


                             }
                         }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                //RESET THE EDITTEXT
                mAmountEnteredEditText.setText("");
            }

        }

    }

    public void setEditTextFilter(){

        mAmountEnteredEditText.setFilters(new InputFilter[] {
                new DigitsKeyListener(Boolean.FALSE, Boolean.TRUE) {
                    int beforeDecimal = 6, afterDecimal = 8;

                    @Override
                    public CharSequence filter(CharSequence source, int start, int end,
                                               Spanned dest, int dstart, int dend) {
                        String temp = mAmountEnteredEditText.getText() + source.toString();

                        if (temp.equals(".")) {
                            return "0.";
                        }else if (temp.equals("0")){
                            return "0.";//if number begins with 0 return decimal place right after
                        }
                        else if (temp.toString().indexOf(".") == -1) {
                            // no decimal point placed yet
                            if (temp.length() > beforeDecimal) {
                                return "";
                            }
                        } else {
                            temp = temp.substring(temp.indexOf(".") + 1);
                            if (temp.length() > afterDecimal) {
                                return "";
                            }
                        }

                        return super.filter(source, start, end, dest, dstart, dend);
                    }
                }
        });

    }
}