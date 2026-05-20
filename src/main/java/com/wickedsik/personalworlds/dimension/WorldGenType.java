package com.wickedsik.personalworlds.dimension;

public enum WorldGenType {
   VOID,
   OVERWORLD,
   FLAT;

   public static WorldGenType fromString(String name) {
      try {
         return valueOf(name.toUpperCase());
      } catch (IllegalArgumentException var2) {
         return VOID;
      }
   }

   // $FF: synthetic method
   private static WorldGenType[] $values() {
      return new WorldGenType[]{VOID, OVERWORLD, FLAT};
   }
}
