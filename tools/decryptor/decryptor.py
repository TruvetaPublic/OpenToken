import argparse
import base64
import csv
from Crypto.Cipher import AES

PROGRAM = 'decryptor.py'
IV = bytes([0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0])
UTF8 = 'utf-8'

unpad = lambda s: s[:-ord(s[len(s) - 1:])]

def decrypt_tokens(key, input_file, output_file):
  with open(output_file, mode = 'w') as outfile:
    columns = [ 'RuleId', 'Token', 'RecordId' ]
    writer = csv.DictWriter(outfile, fieldnames = columns)
    writer.writeheader()

    with open(input_file) as infile:
      reader = csv.DictReader(infile)
      for row in reader:
        token = row['Token'].encode(UTF8)

        # decode the token first
        decoded_token = base64.b64decode(token)

        # decrypt the token
        cipher = AES.new(key.encode(UTF8), AES.MODE_CBC, IV)
        decrypted_text = unpad(cipher.decrypt(decoded_token))

        # write the decrypted token back
        writer.writerow({
          'RuleId': row['RuleId'],
          'Token': decrypted_text.decode(UTF8),
          'RecordId': row['RecordId']
        })

def parse_args():
  parser = argparse.ArgumentParser(
    prog = PROGRAM,
    description = 'Decrypts the tokens'
  )
  parser.add_argument('-e', '--encryption-key', required = True, help = 'Symmetric encryption key.')
  parser.add_argument('-i', '--input-file', required = True, help = 'The input file with encrypted tokens.')
  parser.add_argument('-o', '--output-file', required = True, help = 'The output file with decrypted tokens.')
  return parser.parse_args()

def main():
  try:
    args = parse_args()
    print(f'Encryption key {args.encryption_key}')
    print(f'Input file {args.input_file}')
    print(f'Output file {args.output_file}')
    decrypt_tokens(args.encryption_key, args.input_file, args.output_file)
    print(f'Tokens from {args.input_file} are successfully decrypted and written to {args.output_file}')
  except Exception as e:
    print(f'Failed to decrypt tokens')
    raise

if __name__ == "__main__":
  main()
