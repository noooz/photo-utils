# photo-utils

```
Batch rename, resize and stamp images.

usage: photo-utils --src PATH --dest PATH [OPTIONS]
 -d,--dest <PATH>
    --delete          delete extraneous files from dest dirs
 -h,--help
 -o,--overwrite       overwrite existing files
    --rename          rename files according to their capture date
    --resize <SIZE>   resize image (default: 1900)
 -s,--src <PATH>
    --stamp           write capture date in image

EXAMPLES
copy all photos from dir1 to dir2 and stamp them with their capture date
    photo-utils -s dir1 -d dir2 --stamp

```