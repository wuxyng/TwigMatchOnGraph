package helper;

public class Util {

	
	 public static int getUnsignedByte (byte data){      //��data�ֽ�������ת��Ϊ0~255 (0xFF ��BYTE)��
         return data&0x0FF ;
      }

      public static int getUnsignedByte (short data){      //��data�ֽ�������ת��Ϊ0~65535 (0xFFFF �� WORD)��
            return data&0x0FFFF ;
      }       

     public static long getUnsignedInt (int data){     //��int����ת��Ϊ0~4294967295 (0xFFFFFFFF��DWORD)��
         return data&0x0FFFFFFFF ;
      }
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
