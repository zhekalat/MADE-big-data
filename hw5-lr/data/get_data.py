from sklearn.datasets import fetch_california_housing
from sklearn.model_selection import train_test_split

# Load the dataset
df, y = fetch_california_housing(return_X_y=True, as_frame=True)
df['y'] = y

# Split it to train/val/test
train, test_all = train_test_split(df, train_size=0.75, random_state=42)

# Save it
train.to_csv('train.csv', index=False)
test.to_csv('test.csv', index=False)
