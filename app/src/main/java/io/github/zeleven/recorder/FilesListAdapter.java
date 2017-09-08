package io.github.zeleven.recorder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * The file list adapter for FilesActivity
 */
public class FilesListAdapter extends RecyclerView.Adapter<FilesListAdapter.ViewHolder> {
    private Context mContext;
    private LinearLayoutManager linearLayoutManager;
    private File[] recordingFiles;
    private String filesFolder = Environment.getExternalStorageDirectory().
            getAbsolutePath() + "/Recorder/";

    public FilesListAdapter(Context context, LinearLayoutManager layoutManager) {
        super();
        mContext = context;
        linearLayoutManager = layoutManager;
        // list files in files folder
        recordingFiles = new File(filesFolder).listFiles();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        protected TextView nameText;
        protected TextView durationText;
        protected TextView createTimeText;
        protected LinearLayout listItem;

        public ViewHolder(View itemView) {
            super(itemView);
            // find element in RecyclerView item layout
            nameText = itemView.findViewById(R.id.file_name_text);
            durationText = itemView.findViewById(R.id.file_duration_text);
            createTimeText = itemView.findViewById(R.id.file_create_time_text);
            listItem = itemView.findViewById(R.id.list_item);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(mContext == null) {
            mContext = parent.getContext();
        }
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_recyclerview,
                parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        // get file in folder by position
        final File file = recordingFiles[position];
        // get file's path
        final String filePath = file.getAbsolutePath();

        // create MediaMetadataRetriever object to get the audio file duration
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        metadataRetriever.setDataSource(filePath);
        final long duration = Long.parseLong(metadataRetriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(minutes);

        holder.nameText.setText(file.getName());
        holder.durationText.setText(String.format("%02d:%02d", minutes, seconds));

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        holder.createTimeText.setText(dateFormat.format(new Date(file.lastModified())));

        holder.listItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), "audio/*");
                mContext.startActivity(intent);
            }
        });

        // set long click listener for RecyclerView item
        holder.listItem.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ArrayList<String> dialogText = new ArrayList<>();
                dialogText.add(mContext.getString(R.string.dialog_item_rename));
                dialogText.add(mContext.getString(R.string.dialog_item_delete));
                dialogText.add(mContext.getString(R.string.dialog_item_share));

                CharSequence[] items = dialogText.toArray(new CharSequence[dialogText.size()]);

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(mContext.getString(R.string.dialog_title));
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(i == 0) {
                            // open rename file dialog
                            renameFileDialog(file, position);
                        } else if(i == 1) {
                            // open delete file dialog
                            deleteFileDialog(file, position);
                        } else if(i == 2) {
                            // open share file dialog
                            shareFileDialog(file);
                        }
                    }
                });
                builder.setCancelable(true);
                builder.setNegativeButton(mContext.getString(R.string.dialog_button_cancel),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

                builder.create().show();
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return recordingFiles.length;
    }

    public void renameFileDialog(final File file, final int position) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.rename_dialog, null);
        final EditText nameInput = view.findViewById(R.id.new_filename_input);

        AlertDialog.Builder renameBuilder = new AlertDialog.Builder(mContext);
        renameBuilder.setTitle(mContext.getString(R.string.rename_dialog_title));
        renameBuilder.setCancelable(true);
        renameBuilder.setPositiveButton(mContext.getString(R.string.dialog_button_save),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String newFileName = nameInput.getText().toString().trim() + ".mp3";

                String absFilePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/Recorder/" + newFileName;
                File temp = new File(absFilePath);
                if(temp.exists() && !temp.isDirectory()) {
                    Toast.makeText(mContext, mContext.getString(R.string.toast_filename_existed),
                            Toast.LENGTH_SHORT).show();
                } else {
                    file.renameTo(temp);
                    recordingFiles = new File(filesFolder).listFiles();
                    notifyItemChanged(position);
                }

                dialogInterface.cancel();
            }
        });

        renameBuilder.setNegativeButton(mContext.getString(R.string.dialog_button_cancel),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        renameBuilder.setView(view);
        renameBuilder.create().show();
    }

    public void deleteFileDialog(final File file, final int position) {
        AlertDialog.Builder confirmDelete = new AlertDialog.Builder(mContext);
        confirmDelete.setTitle(mContext.getString(R.string.delete_dialog_title));
        confirmDelete.setMessage(mContext.getString(R.string.delete_dialog_message));
        confirmDelete.setCancelable(true);
        confirmDelete.setPositiveButton(mContext.getString(R.string.dialog_button_ok),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                file.delete();
                recordingFiles = new File(filesFolder).listFiles();
                Toast.makeText(mContext, mContext.getString(R.string.toast_file_deleted),
                        Toast.LENGTH_SHORT).show();
                dialogInterface.cancel();
                notifyItemRemoved(position);
            }
        });

        confirmDelete.setNegativeButton(mContext.getString(R.string.dialog_button_cancel),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        confirmDelete.create().show();
    }

    public void shareFileDialog(File file) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        intent.setType("audio/mp3");
        mContext.startActivity(Intent.createChooser(intent,
                mContext.getString(R.string.share_dialog_title)));
    }
}
