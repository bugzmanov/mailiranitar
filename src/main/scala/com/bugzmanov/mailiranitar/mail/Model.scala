package com.bugzmanov.mailiranitar.mail

import java.time.Instant
import java.util.UUID

case class Message private(id: UUID,
                           sender: String,
                           subject: String,
                           body: String,
                           received: Instant,
                           sent: Instant,
                          ) {
  override def hashCode(): Int = id.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case other: Message if other.id == this.id => true
    case _ => false
  }
}
