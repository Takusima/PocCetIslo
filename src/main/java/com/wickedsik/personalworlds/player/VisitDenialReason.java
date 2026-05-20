package com.wickedsik.personalworlds.player;

public enum VisitDenialReason {
   ALLOWED,
   NOT_INVITED,
   HOST_OFFLINE,
   HOST_NOT_HOME;

   public boolean isAllowed() {
      return this == ALLOWED;
   }

   public boolean isDenied() {
      return this != ALLOWED;
   }

   // $FF: synthetic method
   private static VisitDenialReason[] $values() {
      return new VisitDenialReason[]{ALLOWED, NOT_INVITED, HOST_OFFLINE, HOST_NOT_HOME};
   }
}
