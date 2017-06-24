package com.kunzisoft.remembirthday.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.kunzisoft.remembirthday.R;
import com.kunzisoft.remembirthday.element.Contact;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

/**
 * Adapter linked to contacts with birthday for data feeding
 */
public class ContactAdapter<T extends ContactViewHolder> extends RecyclerView.Adapter<T>{

    private static final String TAG = "ContactBirthdayAdapter";

    private Context context;
    private OnClickItemContactListener onClickItemContactListener;

    private Cursor cursor;
    protected int contactIdColIdx, contactLookupColIdx, contactNameColIdx, contactThumbnailImageUriColIdx, contactImageUriColIdx;

    public ContactAdapter(Context context) {
        this.context = context;
    }

    /**
     * Change cursor implementation for retrieving data
     * @param cursor New cursor
     * @return The old cursor
     */
    public void swapCursor(Cursor cursor) {
        this.cursor = cursor;
        this.contactIdColIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID);
        this.contactLookupColIdx = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
        this.contactNameColIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);
        this.contactThumbnailImageUriColIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);
        this.contactImageUriColIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI);
    }

    public void resetCursor() {
        cursor.close();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemListBuddyView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_contacts, parent, false);
        return (T) new ContactViewHolder(itemListBuddyView);
    }

    @Override
    public void onBindViewHolder(T holder, int position) {
        cursor.moveToPosition(position);

        Contact currentContact = getItemFromCursor(cursor);
        assignDataToView(holder, currentContact);

        if(onClickItemContactListener != null) {
            holder.container.setOnClickListener(new BufferContactClickListener(currentContact, position));
        }
    }

    /**
     * MUST BE REDEFINED
     * Must return a new item based on cursor data
     * @param cursor The cursor at the correct position
     * @return The new item created
     */
    protected Contact getItemFromCursor(Cursor cursor) {
        Contact contact = new Contact(
                cursor.getLong(contactIdColIdx),
                cursor.getString(contactLookupColIdx),
                cursor.getString(contactNameColIdx));
        // Thumbnail
        String uriThumbnailString = cursor.getString(contactThumbnailImageUriColIdx);
        if(uriThumbnailString!=null && !uriThumbnailString.isEmpty()) {
            contact.setImageThumbnailUri(Uri.parse(uriThumbnailString));
        }
        // Photo
        String uriString = cursor.getString(contactImageUriColIdx);
        if(uriString!=null && !uriString.isEmpty())
            contact.setImageUri(Uri.parse(uriString));
        return contact;
    }

    /**
     * MUST BE REDEFINED <br />
     * Method used to link the ViewHolder to the item data
     * @param holder The ViewHolder
     * @param contact The item
     */
    @SuppressWarnings("deprecation")
    protected void assignDataToView(final T holder, Contact contact) {
        if(contact.containsImage()) {
            Picasso.with(context).load(contact.getImageThumbnailUri())
                    .into(holder.icon, new Callback() {
                        @Override
                        public void onSuccess() {
                            cropRoundedImageView(holder.icon);
                        }
                        @Override
                        public void onError() {}
                    });
        } else {
            // Get colors from theme
            TypedValue typedValuePrimary = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.colorPrimary, typedValuePrimary, true);
            int colorPrimary = typedValuePrimary.data;

            // Get the sec text color of the theme
            //TODO color secondary
            TypedValue typedValueSecondary = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.textColorPrimaryInverse, typedValueSecondary, true);
            TypedArray arr = context.obtainStyledAttributes(
                    typedValueSecondary.data, new int[]{android.R.attr.textColorPrimaryInverse});
            int colorSecondary = arr.getColor(0, -1);
            arr.recycle();

            // Colorize content of icon
            holder.icon.setColorFilter(colorPrimary);
            // Colorize background of icon
            Drawable background = holder.icon.getBackground().mutate();
            background.setColorFilter(colorSecondary, PorterDuff.Mode.SRC_ATOP);
            if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                holder.icon.setBackgroundDrawable(background);
            } else {
                holder.icon.setBackground(background);
            }
        }

        holder.name.setText(contact.getName());
    }

    /**
     * Crop ImageView in parameter for create a circle
     */
    private void cropRoundedImageView(ImageView imageView) {
        Bitmap imageBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        RoundedBitmapDrawable imageDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), imageBitmap);
        imageDrawable.setCircular(true);
        imageDrawable.setCornerRadius(Math.max(imageBitmap.getWidth(), imageBitmap.getHeight()) / 2.0f);
        imageView.setImageDrawable(imageDrawable);
    }

    /**
     * Retrieves the listener that handles the event on the click of the item
     * @return The listener
     */
    public OnClickItemContactListener getOnClickItemContactListener() {
        return onClickItemContactListener;
    }

    /**
     * Add click contactBirthday listener to each item
     * @param onClickItemContactListener Listener who defined the `onItemContactClick` method
     */
    public void setOnClickItemContactListener(OnClickItemContactListener onClickItemContactListener) {
        this.onClickItemContactListener = onClickItemContactListener;
    }

    public void setItemChecked(Contact contact) {
        // TODO highlight
    }

    @Override
    public int getItemCount() {
        if(cursor!=null)
            return cursor.getCount();
        else
            return 0;
    }

    /**
     * Class manager for add contact and view in listener
     */
    private class BufferContactClickListener implements View.OnClickListener {

        private Contact contact;
        private int position;

        BufferContactClickListener(Contact contact, int position) {
            this.contact = contact;
            this.position = position;
        }

        @Override
        public void onClick(View view) {
            onClickItemContactListener.onItemContactClick(view, contact, cursor, position);
        }
    }
}
