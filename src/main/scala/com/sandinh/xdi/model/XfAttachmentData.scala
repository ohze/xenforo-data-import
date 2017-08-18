package com.sandinh.xdi.model

case class XfAttachmentData(dataId: Int,
                            userId: Int,
                            uploadDate: Int,
                            filename: String,
                            fileSize: Int, fileHash: String,
                            filePath: String,
                            width: Int,
                            height: Int,
                            thumbnailWidth: Int,
                            thumbnailHeight: Int,
                            attachCount: Int)
