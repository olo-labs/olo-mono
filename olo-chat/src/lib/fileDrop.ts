/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Collect all File objects from a DataTransfer (drag-drop), including files inside dropped folders.
 */

type FileSystemFileEntry = {
  file: (cb: (f: File) => void) => void
}

type FileSystemDirectoryEntry = {
  createReader: () => {
    readEntries: (cb: (entries: (FileSystemFileEntry | FileSystemDirectoryEntry)[]) => void) => void
  }
}

function isDirectoryEntry(
  e: FileSystemFileEntry | FileSystemDirectoryEntry
): e is FileSystemDirectoryEntry {
  return 'createReader' in e && typeof (e as FileSystemDirectoryEntry).createReader === 'function'
}

function readDirectoryEntries(
  dir: FileSystemDirectoryEntry,
  acc: File[]
): Promise<void> {
  const reader = dir.createReader()

  const readBatch = (): Promise<void> =>
    new Promise((resolve, reject) => {
      reader.readEntries((entries: (FileSystemFileEntry | FileSystemDirectoryEntry)[]) => {
        if (entries.length === 0) {
          resolve()
          return
        }
        const promises: Promise<void>[] = []
        for (const entry of entries) {
          if (isDirectoryEntry(entry)) {
            promises.push(readDirectoryEntries(entry, acc))
          } else {
            promises.push(
              new Promise((res) => {
                ;(entry as FileSystemFileEntry).file((file) => {
                  acc.push(file)
                  res()
                })
              })
            )
          }
        }
        Promise.all(promises)
          .then(() => readBatch())
          .then(resolve)
          .catch(reject)
      })
    })

  return readBatch()
}

/** Get all files from dataTransfer; recurses into dropped directories. */
export async function getFilesFromDataTransfer(dataTransfer: DataTransfer): Promise<File[]> {
  const acc: File[] = []
  const items = dataTransfer.items
  if (!items) {
    const files = dataTransfer.files
    if (files) for (let i = 0; i < files.length; i++) acc.push(files[i])
    return acc
  }
  const getAsEntry = (item: DataTransferItem) =>
    'webkitGetAsEntry' in item
      ? (item as DataTransferItem & { webkitGetAsEntry(): FileSystemFileEntry | FileSystemDirectoryEntry | null }).webkitGetAsEntry()
      : null

  for (let i = 0; i < items.length; i++) {
    const entry = getAsEntry(items[i])
    if (!entry) {
      const f = items[i].getAsFile()
      if (f) acc.push(f)
      continue
    }
    const entryTyped = entry as unknown as FileSystemFileEntry | FileSystemDirectoryEntry
    if (isDirectoryEntry(entryTyped)) {
      await readDirectoryEntries(entryTyped, acc)
    } else {
      await new Promise<void>((res) => {
        ;(entryTyped as FileSystemFileEntry).file((file) => {
          acc.push(file)
          res()
        })
      })
    }
  }
  return acc
}
