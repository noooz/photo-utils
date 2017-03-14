# photo-utils

Batch rename, resize and stamp images.

```
usage: photo-utils
 -d,--dest <path>
    --delete          delete extraneous files from dest dirs
 -h,--help
 -o,--overwrite       overwrite existing files
    --rename          rename files according to their capture date
    --resize <size>   resize image (default: 1900)
 -s,--src <path>
    --stamp           write capture date in image
```

## examples

copy all photos from dir1 to dir2 and stamp them with their capture date

	photo-utils -s dir1 -d dir2 --stamp